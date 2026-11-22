import { test, expect } from '@playwright/test';
import { loginAsAdmin, createPage, publishOpenItem, configurePublishing, uploadImageAsset } from '../helpers/cms-ui';
import * as path from 'path';
import { chooseImageFromLibrary } from '../helpers/asset-chooser';
import { placeWidget, saveLayout, openContentTab, openWidgetContentEditor, openWidgetConfigure, confirmWidgetConfigure } from '../helpers/widgets-ui';
import { DTS_URL, DTS_ADMIN_URL, TEST_FOLDER, TEST_ASSET_FOLDER } from '../helpers/qa-env';

/**
 * Facebook Open Graph: adds the og: meta tags to the page. Nothing is visible, so the proof is the <meta property="og:..."> tags in the
 * PUBLISHED page served by the DTS: the values set in Configure, and the overrides typed in the widget's content editor, which win.
 */
const IMAGE = path.join(__dirname, '..', 'fixtures', 'qa-image.png');
const og = (page: import('@playwright/test').Page, property: string) => page.locator(`meta[property="${property}"]`);

test.describe('Facebook Open Graph', () => {
  test.setTimeout(360_000);

  test('Facebook Open Graph: published page carries the og meta tags for the options set in Configure, and Site Name is required', async ({ page }) => {
    const run = Date.now().toString().slice(-7);
    const title = `OG Host ${run}`;
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const host = await createPage(page, title);
    await placeWidget(page, 'percOpenGraph');

    const cfg = await openWidgetConfigure(page, 'percOpenGraph');
    // Site Name is required: Ok is refused while it is empty
    await cfg.getByRole('button', { name: 'Ok' }).click({ timeout: 10_000 });
    await expect(cfg.getByText('Please enter all required fields')).toBeVisible({ timeout: 10_000 });
    await page.screenshot({ path: 'screenshots/facebook-open-graph-site-name-required.png' });
    await cfg.locator('input[name="og_site_name"]').fill(`QA Site ${run}`);
    await cfg.locator('select[name="og_type"]').selectOption('article', { timeout: 10_000 });
    await cfg.locator('input[name="og_image"]').fill(`/QA/og-${run}.png`);
    await cfg.locator('input[name="og_image_width"]').fill('800');
    await cfg.locator('input[name="og_image_height"]').fill('400');
    await cfg.locator('select[name="og_locale"]').selectOption('en_GB', { timeout: 10_000 });
    await cfg.locator('input[name="fb_app_id"]').fill(`99${run}`);
    await page.screenshot({ path: 'screenshots/facebook-open-graph-configure-dialog-filled.png' });
    await confirmWidgetConfigure(page);
    await saveLayout(page);
    await openContentTab(page);
    await publishOpenItem(page);

    await page.goto(`${DTS_URL}/${TEST_FOLDER}/${host}.html`);
    await expect(og(page, 'og:site_name')).toHaveAttribute('content', `QA Site ${run}`);
    await expect(og(page, 'og:type')).toHaveAttribute('content', 'article');
    // the path is made absolute with the site's address (the QA site has no domain, so its name is used as host)
    await expect(og(page, 'og:image')).toHaveAttribute('content', new RegExp(`^https?://.+/QA/og-${run}\.png$`));
    await expect(og(page, 'og:image:width')).toHaveAttribute('content', '800');
    await expect(og(page, 'og:image:height')).toHaveAttribute('content', '400');
    await expect(og(page, 'og:locale')).toHaveAttribute('content', 'en_GB');
    await expect(og(page, 'og:fb_app_id')).toHaveAttribute('content', `99${run}`);
    await expect(og(page, 'og:title'), 'page title is the default og:title').toHaveAttribute('content', title);
    await expect(og(page, 'og:url'), 'page link is the default og:url').toHaveAttribute('content', new RegExp(`${host}\\.html$`));
    const tags = await page.locator('meta[property^="og:"]').evaluateAll(els => els.map(e => `${e.getAttribute('property')} = ${e.getAttribute('content')}`).join('\n'));
    await page.setContent(`<pre style="font:14px monospace">${tags.replace(/</g, '&lt;')}</pre>`);
    await page.screenshot({ path: 'screenshots/facebook-open-graph-published-page-og-meta-tags.png' });
  });

  test('Facebook Open Graph: overrides typed in the content editor replace the Configure values and the page defaults in the published page', async ({ page }) => {
    const run = Date.now().toString().slice(-7);
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const host = await createPage(page, `OG Override Host ${run}`);
    await placeWidget(page, 'percOpenGraph');
    const cfg = await openWidgetConfigure(page, 'percOpenGraph');
    await cfg.locator('input[name="og_site_name"]').fill(`Configured Site ${run}`);
    await confirmWidgetConfigure(page);
    await saveLayout(page);

    const { frame, save } = await openWidgetContentEditor(page, 'percOpenGraph');
    await frame.locator('#perc-content-edit-site_name_override').fill(`Override Site ${run}`);
    await frame.locator('#perc-content-edit-title_override').fill(`Override Title ${run}`);
    await frame.locator('#perc-content-edit-description_override').fill(`Override description ${run}`);
    await frame.locator('#perc-content-edit-url_override').fill(`https://example.com/og/${run}`);
    await frame.locator('#ce_type_override').selectOption('product', { timeout: 10_000 });
    await frame.locator('#perc-content-edit-fb_app_id_override').fill(`77${run}`);
    await page.screenshot({ path: 'screenshots/facebook-open-graph-overrides-content-editor.png' });
    await save.click();
    await expect(save).toBeHidden({ timeout: 30_000 });
    await openContentTab(page);
    await publishOpenItem(page);

    await page.goto(`${DTS_URL}/${TEST_FOLDER}/${host}.html`);
    await expect(og(page, 'og:site_name'), 'site name override wins over Configure').toHaveAttribute('content', `Override Site ${run}`);
    await expect(og(page, 'og:title')).toHaveAttribute('content', `Override Title ${run}`);
    await expect(og(page, 'og:description')).toHaveAttribute('content', `Override description ${run}`);
    await expect(og(page, 'og:type'), 'type override').toHaveAttribute('content', 'product');
    await expect(og(page, 'og:fb_app_id')).toHaveAttribute('content', `77${run}`);
    await expect(page.locator('meta[property="og:url"]')).toHaveAttribute('content', `https://example.com/og/${run}`);
    const tags = await page.locator('meta[property^="og:"]').evaluateAll(els => els.map(e => `${e.getAttribute('property')} = ${e.getAttribute('content')}`).join('\n'));
    await page.setContent(`<pre style="font:14px monospace">${tags.replace(/</g, '&lt;')}</pre>`);
    await page.screenshot({ path: 'screenshots/facebook-open-graph-published-page-override-meta-tags.png' });
  });

  test('Facebook Open Graph: an image chosen from the asset library with its own size replaces the image set in Configure', async ({ page }) => {
    const run = Date.now().toString().slice(-7);
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const asset = await uploadImageAsset(page, TEST_ASSET_FOLDER, IMAGE, `QA OG image ${run}`, `QA OG alt ${run}`);
    await publishOpenItem(page);

    const host = await createPage(page, `OG Image Host ${run}`);
    await placeWidget(page, 'percOpenGraph');
    const cfg = await openWidgetConfigure(page, 'percOpenGraph');
    await cfg.locator('input[name="og_site_name"]').fill(`Configured Site ${run}`);
    await cfg.locator('input[name="og_image"]').fill(`/QA/configured-${run}.png`);
    await confirmWidgetConfigure(page);
    await saveLayout(page);

    const { frame, save } = await openWidgetContentEditor(page, 'percOpenGraph');
    await chooseImageFromLibrary(page, frame.getByRole('button', { name: 'Browse' }).first(), asset);
    await expect(frame.locator('#perc-content-edit-image_override'), 'chosen image is shown in the editor').toHaveValue(new RegExp(asset));
    await frame.locator('#perc-content-edit-image_width_override').fill('320');
    await frame.locator('#perc-content-edit-image_height_override').fill('200');
    await page.screenshot({ path: 'screenshots/facebook-open-graph-image-override-content-editor.png' });
    await save.click();
    await expect(save).toBeHidden({ timeout: 30_000 });
    await openContentTab(page);
    await publishOpenItem(page);

    await page.goto(`${DTS_URL}/${TEST_FOLDER}/${host}.html`);
    const image = (await og(page, 'og:image').getAttribute('content'))!;
    expect(image, 'og:image is the chosen asset, not the Configure image').toContain(asset);
    expect(image).not.toContain('configured-');
    await expect(og(page, 'og:image:width'), 'width override').toHaveAttribute('content', '320');
    await expect(og(page, 'og:image:height'), 'height override').toHaveAttribute('content', '200');
    // the image the tag points to is really served (the site address in the tag is only the site name here, so its path is fetched from the DTS)
    const res = await page.request.get(DTS_URL + new URL(image).pathname);
    expect(res.status(), `GET ${image}`).toBe(200);
    expect(res.headers()['content-type']).toContain('image/png');
    const tags = await page.locator('meta[property^="og:"]').evaluateAll(els => els.map(e => `${e.getAttribute('property')} = ${e.getAttribute('content')}`).join('\n'));
    await page.setContent(`<pre style="font:14px monospace">${tags.replace(/</g, '&lt;')}</pre>`);
    await page.screenshot({ path: 'screenshots/facebook-open-graph-published-page-image-override-meta-tags.png' });
  });
});
