import { test, expect, Page } from '@playwright/test';
import * as path from 'path';
import { loginAsAdmin, createPage, publishOpenItem, configurePublishing, ensureDeliveryResources } from '../helpers/cms-ui';
import { placeWidget, saveLayout, openContentTab, openWidgetContentEditor, openWidgetConfigure, confirmWidgetConfigure } from '../helpers/widgets-ui';
import { DTS_URL, DTS_ADMIN_URL, TEST_FOLDER } from '../helpers/qa-env';

/**
 * Image widget: shows an image uploaded in the widget's content editor, as the image itself or as a thumbnail that opens a lightbox.
 * The image files are written by the CMS when the page is published, so the proof is the PUBLISHED page served by the DTS: the <img>
 * attributes, and the image file the DTS really serves for it.
 */
const IMAGE = path.join(__dirname, '..', 'fixtures', 'qa-image.png');   // 160 x 100 PNG

/** Width and height read from the PNG header of the served file. */
const pngSize = (b: Buffer) => ({ width: b.readUInt32BE(16), height: b.readUInt32BE(20) });

/** New page with an Image widget (optionally configured), image uploaded with the given title/alt and main width, published. Returns the published URL. */
async function publishImagePage(page: Page, run: string, shot: string, opts: { title: string; alt: string; mainWidth?: string; configure?: (form: ReturnType<Page['locator']>) => Promise<void> }): Promise<string> {
  await ensureDeliveryResources(page);
  const host = await createPage(page, `Image Host ${shot.slice(-6)} ${run}`);
  await placeWidget(page, 'percImage');
  if (opts.configure) {
    const form = await openWidgetConfigure(page, 'percImage');
    await opts.configure(form);
    await page.screenshot({ path: `screenshots/${shot}-configure-dialog.png` });
    await confirmWidgetConfigure(page);
  }
  await saveLayout(page);

  const { frame, save } = await openWidgetContentEditor(page, 'percImage');
  await frame.locator('#perc-content-edit-displaytitle').pressSequentially(opts.title, { delay: 10 });
  await frame.locator('#perc-content-edit-alttext').pressSequentially(opts.alt, { delay: 10 });
  await frame.locator('#perc-select-image').setInputFiles(IMAGE);
  await expect(frame.locator('#asset_main_width'), 'size of the chosen image is read').toHaveValue('160', { timeout: 30_000 });
  if (opts.mainWidth) {
    // typed key by key, as a user does: the height box follows the width while 'Constrain proportions' is ticked
    await frame.locator('#asset_main_width').click();
    await frame.locator('#asset_main_width').press('Control+A');
    await frame.locator('#asset_main_width').pressSequentially(opts.mainWidth, { delay: 80 });
    await frame.locator('#asset_main_width').press('Tab');
    await expect(frame.locator('#asset_main_height'), 'height follows the width with constrained proportions').toHaveValue('50');
  }
  await page.screenshot({ path: `screenshots/${shot}-content-editor-image-chosen.png` });
  await save.click();
  await expect(save).toBeHidden({ timeout: 60_000 });
  await openContentTab(page);
  await publishOpenItem(page);
  return `${DTS_URL}/${TEST_FOLDER}/${host}.html`;
}

test.describe('Image widget', () => {
  test.setTimeout(420_000);

  test('Image widget: published page shows the uploaded image with its alt text and title, scaled to the size chosen, and the DTS serves that file', async ({ page }) => {
    const run = Date.now().toString().slice(-7);
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishImagePage(page, run, 'image-widget-scaled', { title: `QA Image ${run}`, alt: `QA alt text ${run}`, mainWidth: '80' });

    await page.goto(url);
    const img = page.locator('img[alt="QA alt text ' + run + '"]');
    await expect(img, 'image with the alt text of the asset').toHaveCount(1);
    await expect(img).toHaveAttribute('title', `QA Image ${run}`);
    await expect(img, 'image has loaded').toHaveJSProperty('complete', true);
    const natural = await img.evaluate((e: HTMLImageElement) => ({ w: e.naturalWidth, h: e.naturalHeight }));
    expect(natural.w, 'image width after scaling to 80').toBe(80);
    expect(natural.h, 'height follows with constrained proportions (160x100 -> 80x50)').toBe(50);
    await expect(img).toHaveAttribute('width', '80');
    await expect(img).toHaveAttribute('height', '50');

    const src = new URL((await img.getAttribute('src'))!, page.url()).toString();
    const res = await page.request.get(src);
    expect(res.status(), `GET ${src}`).toBe(200);
    expect(res.headers()['content-type']).toContain('image/png');
    expect(pngSize(await res.body()), 'size of the file the DTS serves').toEqual({ width: 80, height: 50 });
    await page.screenshot({ path: 'screenshots/image-widget-published-page-shows-the-scaled-image.png' });
  });

  test('Image widget: lightbox rendering shows a thumbnail that opens the full image, with the overrides and policy from Configure', async ({ page }) => {
    const run = Date.now().toString().slice(-7);
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishImagePage(page, run, 'image-widget-lightbox', {
      title: `QA Image ${run}`, alt: `QA asset alt ${run}`,
      configure: async form => {
        await form.locator('select[name="rendering"]').selectOption('lightbox', { timeout: 10_000 });
        await form.locator('input[name="altText"]').fill(`Override alt ${run}`);
        await form.locator('input[name="title"]').fill(`Override title ${run}`);
        await form.locator('input[name="longdesc"]').fill('https://example.com/long-description');
        await form.locator('input[name="ariaLabel"]').fill(`QA aria ${run}`);
        await form.locator('select[name="referalPolicy"]').selectOption('origin', { timeout: 10_000 });
      },
    });

    await page.goto(url);
    const link = page.locator('a[data-lightbox]');
    await expect(link, 'thumbnail link of the lightbox').toHaveCount(1);
    await expect(link, 'title override on the link').toHaveAttribute('title', `Override title ${run}`);
    await expect(link).toHaveAttribute('aria-label', `QA aria ${run}`);
    const thumb = link.locator('img');
    await expect(thumb, 'alt override wins over the alt text of the asset').toHaveAttribute('alt', `Override alt ${run}`);
    await expect(thumb).toHaveAttribute('longdesc', 'https://example.com/long-description');
    await expect(thumb, 'Referrer-Policy from Configure').toHaveAttribute('referrerpolicy', 'origin');
    await expect(thumb, 'thumbnail has loaded').toHaveJSProperty('complete', true);
    const thumbSrc = new URL((await thumb.getAttribute('src'))!, page.url()).toString();
    const fullHref = new URL((await link.getAttribute('href'))!, page.url()).toString();
    expect(thumbSrc, 'thumbnail and full image are different files').not.toBe(fullHref);
    expect((await page.request.get(thumbSrc)).status(), 'thumbnail is served').toBe(200);
    expect((await page.request.get(fullHref)).status(), 'full image is served').toBe(200);

    await thumb.click();
    await expect(page.locator('#lightbox, .lightbox, #lightboxOverlay').first(), 'lightbox opens on click').toBeVisible({ timeout: 15_000 });
    const big = page.locator('#lightbox img.lb-image');
    await expect(big, 'the full image is shown in the lightbox').toBeVisible({ timeout: 30_000 });
    await expect(big).toHaveAttribute('src', await link.getAttribute('href') as string);
    await expect.poll(() => big.evaluate((e: HTMLImageElement) => e.naturalWidth), { message: 'lightbox shows the full 160 px wide image', timeout: 15_000 }).toBe(160);
    await page.screenshot({ path: 'screenshots/image-widget-lightbox-opens-the-full-image.png' });
  });
});
