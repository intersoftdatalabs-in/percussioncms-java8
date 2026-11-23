import * as path from 'path';
import { test, expect, Page } from '@playwright/test';
import { loginAsAdmin, createPage, publishOpenItem, configurePublishing, uploadImageAsset } from '../helpers/cms-ui';
import { chooseImageFromLibrary } from '../helpers/asset-chooser';
import { placeWidget, saveLayout, openContentTab, openWidgetContentEditor, openWidgetConfigure, confirmWidgetConfigure } from '../helpers/widgets-ui';
import { DTS_URL, DTS_ADMIN_URL, TEST_FOLDER, TEST_ASSET_FOLDER } from '../helpers/qa-env';

/**
 * X (Twitter) Summary Cards: adds the twitter: meta tags to the page. Nothing is visible, so the proof is the <meta name="twitter:..."> tags in the
 * PUBLISHED page served by the DTS: the values set in Configure, the card type (a card with no image is always a plain "summary"), and the overrides
 * typed in the widget's content editor, which win over Configure and over the page's own title.
 */
const IMAGE = path.join(__dirname, '..', 'fixtures', 'qa-image.png');
const run = Date.now().toString().slice(-7);
const tw = (page: Page, name: string) => page.locator(`meta[name="${name}"]`);

/** A new page with the widget, configured through `configure`, optionally with content-editor overrides; published. Returns the page title and published URL. */
async function publishCardPage(page: Page, shot: string, configure: (cfg: ReturnType<Page['locator']>) => Promise<void>,
                               overrides?: (frame: ReturnType<Page['frameLocator']>) => Promise<void>) {
  const title = `Twitter Host ${shot.slice(-9)} ${run}`;
  const host = await createPage(page, title);
  await placeWidget(page, 'percTwitterSummaryCards');
  const cfg = await openWidgetConfigure(page, 'percTwitterSummaryCards');
  await configure(cfg);
  await page.screenshot({ path: `screenshots/${shot}-configure-dialog.png` });
  await confirmWidgetConfigure(page);
  await saveLayout(page);
  if (overrides) {
    const { frame, save } = await openWidgetContentEditor(page, 'percTwitterSummaryCards');
    await overrides(frame);
    await page.screenshot({ path: `screenshots/${shot}-content-editor-overrides.png` });
    await save.click();
    await expect(save).toBeHidden({ timeout: 30_000 });
  }
  await openContentTab(page);
  await publishOpenItem(page);
  return { title, url: `${DTS_URL}/${TEST_FOLDER}/${host}.html` };
}

/** Shows the twitter: meta tags as text so that the screenshot proves them. */
async function showTags(page: Page, file: string) {
  const tags = await page.locator('meta[name^="twitter:"]').evaluateAll(els => els.map(e => `${e.getAttribute('name')} = ${e.getAttribute('content')}`).join('\n'));
  await page.setContent(`<pre style="font:14px monospace">${tags.replace(/</g, '&lt;')}</pre>`);
  await page.screenshot({ path: `screenshots/${file}.png` });
}

test.describe('X (Twitter) Summary Cards', () => {
  test.setTimeout(150_000);

  test('Twitter Summary Cards: the Configure values (large image card, @username, default image and alt text) and the page title are written as twitter meta tags', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const { title, url } = await publishCardPage(page, 'twitter-cards-configure', async cfg => {
      await cfg.locator('input[name="twitter_site"]').fill(`@qasite${run}`);
      await cfg.locator('input[name="twitter_image"]').fill(`/QA/card-${run}.png`);
      await cfg.locator('input[name="twitter_image_alt"]').fill(`QA card alt ${run}`);
    });
    await page.goto(url);
    await expect(tw(page, 'twitter:card'), 'default card type is the large image card').toHaveAttribute('content', 'summary_large_image');
    await expect(tw(page, 'twitter:site')).toHaveAttribute('content', `@qasite${run}`);
    await expect(tw(page, 'twitter:title'), 'the page title is the default title').toHaveAttribute('content', title);
    await expect(tw(page, 'twitter:image'), 'image path made absolute').toHaveAttribute('content', new RegExp(`^https?://.+/QA/card-${run}\\.png$`));
    await expect(tw(page, 'twitter:image:alt')).toHaveAttribute('content', `QA card alt ${run}`);
    await showTags(page, 'twitter-cards-published-page-meta-tags');
  });

  test('Twitter Summary Cards: card type "Summary Card" is written as summary', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const { url } = await publishCardPage(page, 'twitter-cards-summary-type', async cfg => {
      await cfg.locator('select[name="twittercard_type"]').selectOption('summary', { timeout: 10_000 });
      await cfg.locator('input[name="twitter_image"]').fill(`/QA/card-${run}.png`);
    });
    await page.goto(url);
    await expect(tw(page, 'twitter:card')).toHaveAttribute('content', 'summary');
    await expect(tw(page, 'twitter:image'), 'the image is still written').toHaveAttribute('content', new RegExp(`card-${run}\\.png$`));
  });

  test('Twitter Summary Cards: a large image card with no image is written as a plain summary card and has no image tag', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const { url } = await publishCardPage(page, 'twitter-cards-no-image', async cfg => {
      await cfg.locator('select[name="twittercard_type"]').selectOption('summary_large_image', { timeout: 10_000 });
    });
    await page.goto(url);
    await expect(tw(page, 'twitter:card'), 'no image: falls back to summary').toHaveAttribute('content', 'summary');
    await expect(tw(page, 'twitter:image'), 'no image tag').toHaveCount(0);
    await showTags(page, 'twitter-cards-published-page-no-image-meta-tags');
  });

  test('Twitter Summary Cards: overrides typed in the content editor replace the Configure values and the page title', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const { url } = await publishCardPage(page, 'twitter-cards-overrides', async cfg => {
      await cfg.locator('input[name="twitter_site"]').fill(`@configured${run}`);
      await cfg.locator('input[name="twitter_image"]').fill(`/QA/card-${run}.png`);
      await cfg.locator('input[name="twitter_image_alt"]').fill(`Configured alt ${run}`);
    }, async frame => {
      await frame.locator('#perc-content-edit-twitter_site_override').fill(`@override${run}`);
      await frame.locator('#perc-content-edit-title_override').fill(`Override title ${run}`);
      await frame.locator('#perc-content-edit-description_override').fill(`Override description ${run}`);
      await frame.locator('#perc-content-edit-image_alt_override').fill(`Override alt ${run}`);
    });
    await page.goto(url);
    await expect(tw(page, 'twitter:site'), 'site override wins').toHaveAttribute('content', `@override${run}`);
    await expect(tw(page, 'twitter:title')).toHaveAttribute('content', `Override title ${run}`);
    await expect(tw(page, 'twitter:description')).toHaveAttribute('content', `Override description ${run}`);
    await expect(tw(page, 'twitter:image:alt')).toHaveAttribute('content', `Override alt ${run}`);
    await showTags(page, 'twitter-cards-published-page-override-meta-tags');
  });

  test('Twitter Summary Cards: an image chosen from the asset library replaces the Configure image and is served', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const asset = await uploadImageAsset(page, TEST_ASSET_FOLDER, IMAGE, `QA card image ${run}`, `QA card alt ${run}`);
    await publishOpenItem(page);
    const { url } = await publishCardPage(page, 'twitter-cards-image-override', async cfg => {
      await cfg.locator('input[name="twitter_image"]').fill(`/QA/configured-${run}.png`);
    }, async frame => {
      await chooseImageFromLibrary(page, frame.getByRole('button', { name: 'Browse' }).first(), asset);
    });
    await page.goto(url);
    const image = (await tw(page, 'twitter:image').getAttribute('content'))!;
    expect(image, 'twitter:image is the chosen asset, not the Configure image').toContain(asset);
    expect(image).not.toContain('configured-');
    const res = await page.request.get(DTS_URL + new URL(image).pathname);
    expect(res.status(), `GET ${image}`).toBe(200);
    expect(res.headers()['content-type']).toContain('image/png');
    await showTags(page, 'twitter-cards-published-page-image-override-meta-tags');
  });
});
