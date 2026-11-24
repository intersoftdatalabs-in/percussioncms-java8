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
process.env.QA_IAL_RUN ??= Date.now().toString().slice(-7);   // shared with the workers Playwright restarts after a failed scenario
const run = process.env.QA_IAL_RUN;
const token = `QA IAL ${run}`;
const names = ['Alpha', 'Bravo', 'Charlie'];
const titleOf = (n: string) => `${token} ${n}`;
const altOf = (n: string) => `QA IAL alt ${run} ${n}`;
const captionOf = (n: string) => `QA IAL caption ${run} ${n}`;
const unrelatedAlt = `Unrelated image alt ${run}`;

/** Puts an Image Auto-List on a new page, optionally configures it, sets the criteria (title contains the token, the QA asset folder) and publishes. */
async function publishListPage(page: Page, shot: string, configure?: (form: ReturnType<Page['locator']>) => Promise<void>, titleContains: string = token, withTitle = true): Promise<string> {
  const host = await createPage(page, `Image List Host ${shot.replace(/^image-auto-list-/, '')} ${run}`);
  await placeWidget(page, 'percImageAutoList');
  if (configure) {
    const form = await openWidgetConfigure(page, 'percImageAutoList');
    await configure(form);
    await page.screenshot({ path: `screenshots/${shot}-configure-dialog.png` });
    await confirmWidgetConfigure(page);
  }
  await saveLayout(page);

  const { frame, save } = await openWidgetContentEditor(page, 'percImageAutoList');
  if (withTitle) await frame.locator('#perc-content-edit-displaytitle').pressSequentially(`Images ${run}`, { delay: 10 });
  await frame.locator('#display_title_contains').pressSequentially(titleContains, { delay: 10 });
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

  test('Image Auto-List: thumbnails with the list title format, div id, role and horizontal layout from Configure are written', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishListPage(page, 'image-auto-list-thumbnails-formats-horizontal', async f => {
      await f.locator('select[name="renderingformat"]').selectOption('thumbnail', { timeout: 10_000 });
      await f.locator('select[name="listTitleFormat"]').selectOption('h3', { timeout: 10_000 });
      await f.locator('input[name="autoListDivId"]').fill(`qaial${run}`);
      await f.locator('input[name="listAriaRole"]').fill('region');
      await f.locator('select[name="layout"]').selectOption('perc-list-horizontal', { timeout: 10_000 });
    });
    await page.goto(url);
    const box = page.locator(`#qaial${run}`);
    await expect(box, 'list div id').toHaveCount(1, { timeout: 30_000 });
    await expect(box.locator('h3').filter({ hasText: `Images ${run}` }), 'list title is a Heading 3').toHaveCount(1);
    await expect(box, 'a list with a title is labelled by that title').toHaveAttribute('aria-labelledby', /.+/);
    await expect(box, 'role').toHaveAttribute('role', 'region');
    await expect(box, 'horizontal layout class').toHaveClass(/perc-list-horizontal/);
    await expect(box.locator('li img'), 'three thumbnails').toHaveCount(3);
    await expect(box.locator('a'), 'thumbnails are not links').toHaveCount(0);
    const thumb = new URL((await box.locator('li img').first().getAttribute('src'))!, page.url()).toString();
    expect((await page.request.get(thumb)).status(), 'thumbnail is served').toBe(200);
    await page.screenshot({ path: 'screenshots/image-auto-list-published-page-thumbnails-formats-horizontal.png' });
  });

  test('Image Auto-List: the link opens in a new window, and the aria label from Configure is written when the list has no title', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishListPage(page, 'image-auto-list-new-window-aria-label', async f => {
      await f.locator('select[name="renderingformat"]').selectOption('thumbnail_link', { timeout: 10_000 });
      await f.locator('select[name="target"]').selectOption('_blank', { timeout: 10_000 });
      await f.locator('input[name="listAriaLabel"]').fill('QA images list');
    }, token, false);
    await page.goto(url);
    const box = page.locator('.perc-image-auto-list');
    await expect(box.locator('li a').first(), 'link target').toHaveAttribute('target', '_blank', { timeout: 30_000 });
    await expect(box, 'aria label').toHaveAttribute('aria-label', 'QA images list');
    await expect(box, 'no labelledby without a title').not.toHaveAttribute('aria-labelledby', /.*/);
    await page.screenshot({ path: 'screenshots/image-auto-list-published-page-new-window-aria-label-without-title.png' });
  });

  test('Image Auto-List: sorted by title the images are in title order, ascending and descending', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const order = async () => (await page.locator(`img[alt^="QA IAL alt ${run}"]`).evaluateAll(els => els.map(e => e.getAttribute('title') || ''))).map(t => t.replace(`${token} `, ''));
    const asc = await publishListPage(page, 'image-auto-list-sort-title-asc', async f => {
      await f.locator('select[name="sortby"]').selectOption('rx:displaytitle', { timeout: 10_000 });
      await f.locator('select[name="sortDirection"]').selectOption('asc', { timeout: 10_000 });
    });
    await page.goto(asc);
    await expect(page.locator(`img[alt="${altOf('Alpha')}"]`)).toHaveCount(1, { timeout: 30_000 });
    expect(await order(), 'ascending by title').toEqual(['Alpha', 'Bravo', 'Charlie']);
    await page.screenshot({ path: 'screenshots/image-auto-list-published-page-sorted-by-title-ascending.png' });
    const desc = await publishListPage(page, 'image-auto-list-sort-title-desc', async f => {
      await f.locator('select[name="sortby"]').selectOption('rx:displaytitle', { timeout: 10_000 });
      await f.locator('select[name="sortDirection"]').selectOption('desc', { timeout: 10_000 });
    });
    await page.goto(desc);
    await expect(page.locator(`img[alt="${altOf('Alpha')}"]`)).toHaveCount(1, { timeout: 30_000 });
    expect(await order(), 'descending by title').toEqual(['Charlie', 'Bravo', 'Alpha']);
    await page.screenshot({ path: 'screenshots/image-auto-list-published-page-sorted-by-title-descending.png' });
  });

  test('Image Auto-List: the post date is shown with the format and the French locale from Configure', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishListPage(page, 'image-auto-list-post-date-french', async f => {
      await f.locator('input[name="showPostDate"]').check();
      await f.locator('input[name="postDateTimeFormat"]').fill('EEEE d MMMM yyyy');
      await f.locator('select[name="locale"]').selectOption('fr-FR', { timeout: 10_000 });
    });
    await page.goto(url);
    const dates = page.locator('li .perc-image-auto-list-date');
    await expect(dates, 'a post date for each of the three images').toHaveCount(3, { timeout: 30_000 });
    await expect(dates.first(), 'date in French with the chosen format').toHaveText(/^(lundi|mardi|mercredi|jeudi|vendredi|samedi|dimanche) \d{1,2} \p{L}+ \d{4}$/u);
    await page.screenshot({ path: 'screenshots/image-auto-list-published-page-post-date-in-french.png' });
  });

  test('Image Auto-List: "Hide auto list when empty" hides a list without images and leaves it visible when the option is off', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const none = `NoSuchImage ${run}`;
    const hidden = await publishListPage(page, 'image-auto-list-hide-when-empty-on', async f => {
      await f.locator('input[name="perc_hide_when_empty"]').check();
    }, none);
    await page.goto(hidden);
    await expect(page.locator('.perc-image-auto-list'), 'the empty list is marked hidden').toHaveAttribute('hidden', '', { timeout: 30_000 });
    await expect(page.locator('.perc-image-auto-list'), 'and is not shown').toBeHidden();
    await page.screenshot({ path: 'screenshots/image-auto-list-published-page-empty-list-hidden.png' });
    const shown = await publishListPage(page, 'image-auto-list-hide-when-empty-off', async f => {
      await f.locator('input[name="perc_hide_when_empty"]').uncheck();
    }, none);
    await page.goto(shown);
    await expect(page.locator('.perc-image-auto-list'), 'with the option off the empty list is in the page').toHaveCount(1, { timeout: 30_000 });
    await expect(page.locator('.perc-image-auto-list'), 'and has no hidden attribute').not.toHaveAttribute('hidden', /.*/);
  });

  test('Image Auto-List: the list item format from Configure is written inside each list item', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishListPage(page, 'image-auto-list-item-format', async f => {
      await f.locator('select[name="listItemFormat"]').selectOption('h4', { timeout: 10_000 });
    });
    await page.goto(url);
    const items = page.locator(`li.perc-list-element`).filter({ has: page.locator(`img[alt^="QA IAL alt ${run}"]`) });
    await expect(items, 'three list items').toHaveCount(3, { timeout: 30_000 });
    await expect(items.locator('h4 img'), 'the image of each item is wrapped in a Heading 4 inside the list item').toHaveCount(3);
    await expect(page.locator('ul.perc-list-main > h4'), 'no heading is written between the list and its items').toHaveCount(0);
    await page.screenshot({ path: 'screenshots/image-auto-list-published-page-item-format-heading.png' });
  });
});
