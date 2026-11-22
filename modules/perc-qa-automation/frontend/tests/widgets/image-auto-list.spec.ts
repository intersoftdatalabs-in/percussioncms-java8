import { test, expect, Page } from '@playwright/test';
import * as path from 'path';
import { loginAsAdmin, createPage, publishOpenItem, configurePublishing, uploadImageAsset } from '../helpers/cms-ui';
import { placeWidget, saveLayout, openContentTab, openWidgetContentEditor, openWidgetConfigure, confirmWidgetConfigure } from '../helpers/widgets-ui';
import { DTS_URL, DTS_ADMIN_URL, TEST_FOLDER, TEST_ASSET_FOLDER } from '../helpers/qa-env';

/**
 * Image Auto-List: lists the Image assets that match criteria set in the widget's content editor (title contains, asset folder), as images,
 * thumbnails or thumbnail links. The list is built by the CMS when the page is published, so the proof is the PUBLISHED page served by the
 * DTS. Four images are uploaded and published first: three whose title contains the run token and one that does not.
 */
const IMAGE = path.join(__dirname, '..', 'fixtures', 'qa-image.png');   // 160 x 100 PNG
const run = Date.now().toString().slice(-7);
const token = `QA IAL ${run}`;
const names = ['Alpha', 'Bravo', 'Charlie'];
const titleOf = (n: string) => `${token} ${n}`;
const altOf = (n: string) => `QA IAL alt ${run} ${n}`;
const captionOf = (n: string) => `QA IAL caption ${run} ${n}`;
const unrelatedAlt = `Unrelated image alt ${run}`;

/** Puts an Image Auto-List on a new page, optionally configures it, sets the criteria (title contains the token, the QA asset folder) and publishes. */
async function publishListPage(page: Page, shot: string, configure?: (form: ReturnType<Page['locator']>) => Promise<void>): Promise<string> {
  const host = await createPage(page, `Image List Host ${shot.slice(-7)} ${run}`);
  await placeWidget(page, 'percImageAutoList');
  if (configure) {
    const form = await openWidgetConfigure(page, 'percImageAutoList');
    await configure(form);
    await page.screenshot({ path: `screenshots/${shot}-configure-dialog.png` });
    await confirmWidgetConfigure(page);
  }
  await saveLayout(page);

  const { frame, save } = await openWidgetContentEditor(page, 'percImageAutoList');
  await frame.locator('#perc-content-edit-displaytitle').pressSequentially(`Images ${run}`, { delay: 10 });
  await frame.locator('#display_title_contains').pressSequentially(token, { delay: 10 });
  await frame.locator('.fancytree-expander').first().click({ timeout: 15_000 });
  await frame.locator('.fancytree-title', { hasText: new RegExp(`^${TEST_ASSET_FOLDER}$`) }).click({ timeout: 15_000 });
  await page.screenshot({ path: `screenshots/${shot}-content-editor-criteria.png` });
  await save.click();
  await expect(save).toBeHidden({ timeout: 60_000 });
  await openContentTab(page);
  await publishOpenItem(page);
  return `${DTS_URL}/${TEST_FOLDER}/${host}.html`;
}

test.describe.serial('Image Auto-List', () => {
  test.setTimeout(600_000);

  test('Image Auto-List: published page lists the image assets whose title contains the text, newest first, and not the others', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    // auto-lists only show published assets: each image is published right after the upload
    for (const n of names) {
      await uploadImageAsset(page, TEST_ASSET_FOLDER, IMAGE, titleOf(n), altOf(n), captionOf(n));
      await publishOpenItem(page);
    }
    await uploadImageAsset(page, TEST_ASSET_FOLDER, IMAGE, `Unrelated image ${run}`, unrelatedAlt);
    await publishOpenItem(page);
    const url = await publishListPage(page, 'image-auto-list-title-filter');

    await page.goto(url);
    for (const n of names) await expect(page.locator(`img[alt="${altOf(n)}"]`), `"${titleOf(n)}" is listed`).toHaveCount(1, { timeout: 30_000 });
    await expect(page.locator(`img[alt="${unrelatedAlt}"]`), 'image whose title does not match is not listed').toHaveCount(0);
    await expect(page.getByText(`Images ${run}`).first(), 'list title').toBeVisible();
    // default sort is newest first: Charlie (uploaded last) is above Bravo above Alpha
    const alts = await page.locator(`img[alt^="QA IAL alt ${run}"]`).evaluateAll(els => els.map(e => e.getAttribute('alt')));
    expect(alts, 'newest first').toEqual([altOf('Charlie'), altOf('Bravo'), altOf('Alpha')]);
    for (const n of names) {
      const img = page.locator(`img[alt="${altOf(n)}"]`);
      await expect(img).toHaveAttribute('title', titleOf(n));
      await expect(img, 'image file loads').toHaveJSProperty('complete', true);
      expect(await img.evaluate((e: HTMLImageElement) => e.naturalWidth), 'served image is 160 px wide').toBe(160);
    }
    await page.screenshot({ path: 'screenshots/image-auto-list-published-page-lists-matching-images.png' });
  });

  test('Image Auto-List: thumbnail links, max list length and captions from Configure are applied to the published list', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishListPage(page, 'image-auto-list-thumbnail-links', async form => {
      await form.locator('select[name="renderingformat"]').selectOption('thumbnail_link', { timeout: 10_000 });
      await form.locator('input[name="maxlength"]').fill('2');
      await form.locator('input[name="caption"]').check();
    });

    await page.goto(url);
    const items = page.locator('li.perc-list-element').filter({ has: page.locator(`img[alt^="QA IAL alt ${run}"]`) });
    await expect(items, 'only 2 of the 3 matching images are listed').toHaveCount(2, { timeout: 30_000 });
    await expect(items.locator('a > img'), 'each image is a thumbnail inside a link').toHaveCount(2);
    await expect(items.locator('.perc-caption'), 'a caption block for each image').toHaveCount(2);
    // the caption is the Link text of the asset (newest first: Charlie, Bravo)
    await expect(items.locator('.perc-caption')).toHaveText([captionOf('Charlie'), captionOf('Bravo')].map(t => new RegExp(t)));
    const link = items.first().locator('a');
    const href = new URL((await link.getAttribute('href'))!, page.url()).toString();
    const thumb = new URL((await link.locator('img').getAttribute('src'))!, page.url()).toString();
    expect(href, 'link goes to the full image, not to the thumbnail').not.toBe(thumb);
    expect((await page.request.get(href)).status(), 'full image is served').toBe(200);
    expect((await page.request.get(thumb)).status(), 'thumbnail is served').toBe(200);
    await page.screenshot({ path: 'screenshots/image-auto-list-published-page-thumbnail-links-limited-to-two.png' });
  });
});
