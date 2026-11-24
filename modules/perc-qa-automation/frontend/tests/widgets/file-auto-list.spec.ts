import { test, expect, Page } from '@playwright/test';
import * as path from 'path';
import { loginAsAdmin, createPage, publishOpenItem, configurePublishing, uploadFileAsset } from '../helpers/cms-ui';
import { placeWidget, saveLayout, openContentTab, openWidgetContentEditor, openWidgetConfigure, confirmWidgetConfigure } from '../helpers/widgets-ui';
import { DTS_URL, DTS_ADMIN_URL, TEST_FOLDER, TEST_ASSET_FOLDER } from '../helpers/qa-env';

/**
 * File Auto-List: lists the File assets that match criteria set in the widget's content editor (title contains, asset folder).
 * The list is built by the CMS when the page is published, so the proof is the PUBLISHED page served by the DTS. Four PDF assets are
 * uploaded first: three whose title contains the run token and one that does not.
 */
const PDF = path.join(__dirname, '..', 'fixtures', 'test.pdf');
process.env.QA_FAL_RUN ??= Date.now().toString().slice(-7);   // shared with the workers Playwright restarts after a failed scenario
const run = process.env.QA_FAL_RUN;
const token = `QA FAL ${run}`;
const titles = ['Alpha', 'Bravo', 'Charlie'].map(n => `${token} ${n}`);
const unrelated = `Unrelated asset ${run}`;

/** Puts a File Auto-List on a new page, optionally configures it, sets the criteria (title contains the token, the QA asset folder) and publishes. */
async function publishListPage(page: Page, shot: string, configure?: (form: ReturnType<Page['locator']>) => Promise<void>, titleContains: string = token, withTitle = true): Promise<string> {
  const host = await createPage(page, `File List Host ${shot.slice(-6)} ${run}`);
  await placeWidget(page, 'percFileAutoList');
  if (configure) {
    const form = await openWidgetConfigure(page, 'percFileAutoList');
    await configure(form);
    await page.screenshot({ path: `screenshots/${shot}-configure-dialog.png` });
    await confirmWidgetConfigure(page);
  }
  await saveLayout(page);

  const { frame, save } = await openWidgetContentEditor(page, 'percFileAutoList');
  if (withTitle) await frame.locator('#perc-content-edit-displaytitle').pressSequentially(`Files ${run}`, { delay: 10 });
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

test.describe.serial('File Auto-List', () => {
  test.setTimeout(480_000);

  test('File Auto-List: published page lists the file assets whose title contains the text, from the chosen asset folder, and not the others', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    // auto-lists only show assets that have been published, so each asset is published right after the upload
    for (const t of [...titles, unrelated]) {
      await uploadFileAsset(page, TEST_ASSET_FOLDER, PDF, t);
      await publishOpenItem(page);
    }
    const url = await publishListPage(page, 'file-auto-list-title-filter');

    await page.goto(url);
    const list = page.locator('.perc-file-auto-list, .perc-list-vertical').first();
    for (const t of titles) await expect(page.getByRole('link', { name: t }), `"${t}" is listed`).toBeVisible({ timeout: 30_000 });
    await expect(page.getByRole('link', { name: unrelated }), 'asset whose title does not match is not listed').toHaveCount(0);
    await expect(page.getByText(`Files ${run}`).first(), 'list title').toBeVisible();
    await page.screenshot({ path: 'screenshots/file-auto-list-published-page-lists-matching-files.png' });
    expect(await list.count()).toBeGreaterThan(0);
  });

  test('File Auto-List: max list length and the list type from Configure are applied to the published list', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishListPage(page, 'file-auto-list-max-and-ordered', async form => {
      await form.locator('select[name="listType"]').selectOption('ordered', { timeout: 10_000 });
      await form.locator('input[name="maxlength"]').fill('2');
    });

    await page.goto(url);
    const items = page.locator('ol li').filter({ has: page.getByRole('link', { name: new RegExp(token) }) });
    await expect(items, 'only 2 of the 3 matching files are listed, as an ordered list').toHaveCount(2, { timeout: 30_000 });
    await page.screenshot({ path: 'screenshots/file-auto-list-published-page-ordered-list-limited-to-two.png' });
  });

  test('File Auto-List: list title format, div id, item format, role, horizontal layout and link target from Configure are written', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishListPage(page, 'file-auto-list-formats-aria-layout', async f => {
      await f.locator('select[name="listTitleFormat"]').selectOption('h3', { timeout: 10_000 });
      await f.locator('input[name="autoListDivId"]').fill(`qafal${run}`);
      await f.locator('select[name="listItemFormat"]').selectOption('h4', { timeout: 10_000 });
      await f.locator('input[name="listAriaRole"]').fill('region');
      await f.locator('select[name="layout"]').selectOption('perc-list-horizontal', { timeout: 10_000 });
      await f.locator('select[name="target"]').selectOption('_blank', { timeout: 10_000 });
    });
    await page.goto(url);
    const box = page.locator(`#qafal${run}`);
    await expect(box, 'list div id').toHaveCount(1, { timeout: 30_000 });
    await expect(box.locator(`h3`).filter({ hasText: `Files ${run}` }), 'list title is a Heading 3').toHaveCount(1);
    await expect(box, 'a list with a title is labelled by that title').toHaveAttribute('aria-labelledby', /.+/);
    await expect(box, 'role').toHaveAttribute('role', 'region');
    await expect(box, 'horizontal layout class').toHaveClass(/perc-list-horizontal/);
    await expect(box.locator('li h4 a', { hasText: token }), 'each item is wrapped in a Heading 4').toHaveCount(3);
    await expect(box.locator('li h4 a', { hasText: token }).first(), 'link target').toHaveAttribute('target', '_blank');
    await page.screenshot({ path: 'screenshots/file-auto-list-published-page-formats-aria-horizontal-new-window.png' });
  });

  test('File Auto-List: the aria label from Configure is written when the list has no title', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishListPage(page, 'file-auto-list-aria-label-no-title', async f => {
      await f.locator('input[name="listAriaLabel"]').fill('QA files list');
    }, token, false);
    await page.goto(url);
    const box = page.locator('.perc-file-auto-list');
    await expect(box, 'aria label').toHaveAttribute('aria-label', 'QA files list', { timeout: 30_000 });
    await expect(box, 'no labelledby without a title').not.toHaveAttribute('aria-labelledby', /.*/);
    await page.screenshot({ path: 'screenshots/file-auto-list-published-page-aria-label-without-title.png' });
  });

  test('File Auto-List: sorted by title the files are in title order, ascending and descending', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const order = async () => (await page.locator('.perc-file-auto-list li a').allInnerTexts()).filter(t => t.startsWith(token)).map(t => t.replace(`${token} `, ''));
    const asc = await publishListPage(page, 'file-auto-list-sort-title-asc', async f => {
      await f.locator('select[name="sortby"]').selectOption('rx:displaytitle', { timeout: 10_000 });
      await f.locator('select[name="sortDirection"]').selectOption('asc', { timeout: 10_000 });
    });
    await page.goto(asc);
    await expect(page.getByRole('link', { name: titles[0] })).toBeVisible({ timeout: 30_000 });
    expect(await order(), 'ascending by title').toEqual(['Alpha', 'Bravo', 'Charlie']);
    await page.screenshot({ path: 'screenshots/file-auto-list-published-page-sorted-by-title-ascending.png' });
    const desc = await publishListPage(page, 'file-auto-list-sort-title-desc', async f => {
      await f.locator('select[name="sortby"]').selectOption('rx:displaytitle', { timeout: 10_000 });
      await f.locator('select[name="sortDirection"]').selectOption('desc', { timeout: 10_000 });
    });
    await page.goto(desc);
    await expect(page.getByRole('link', { name: titles[0] })).toBeVisible({ timeout: 30_000 });
    expect(await order(), 'descending by title').toEqual(['Charlie', 'Bravo', 'Alpha']);
    await page.screenshot({ path: 'screenshots/file-auto-list-published-page-sorted-by-title-descending.png' });
  });

  test('File Auto-List: the post date is shown with the format and the French locale from Configure', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishListPage(page, 'file-auto-list-post-date-french', async f => {
      await f.locator('input[name="showPostDate"]').check();
      await f.locator('input[name="postDateTimeFormat"]').fill('EEEE d MMMM yyyy');
      await f.locator('select[name="locale"]').selectOption('fr-FR', { timeout: 10_000 });
    });
    await page.goto(url);
    const dates = page.locator('.perc-file-auto-list li .perc-file-auto-list-date');
    await expect(dates, 'a post date for each of the three files').toHaveCount(3, { timeout: 30_000 });
    await expect(dates.first(), 'date in French with the chosen format').toHaveText(/^(lundi|mardi|mercredi|jeudi|vendredi|samedi|dimanche) \d{1,2} \p{L}+ \d{4}$/u);
    await page.screenshot({ path: 'screenshots/file-auto-list-published-page-post-date-in-french.png' });
  });

  test('File Auto-List: "Hide auto list when empty" hides a list without files and leaves it visible when the option is off', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const none = `NoSuchFile ${run}`;
    const hidden = await publishListPage(page, 'file-auto-list-hide-when-empty-on', async f => {
      await f.locator('input[name="perc_hide_when_empty"]').check();
    }, none);
    await page.goto(hidden);
    await expect(page.locator('.perc-file-auto-list'), 'the empty list is marked hidden').toHaveAttribute('hidden', '', { timeout: 30_000 });
    await expect(page.locator('.perc-file-auto-list'), 'and is not shown').toBeHidden();
    await page.screenshot({ path: 'screenshots/file-auto-list-published-page-empty-list-hidden.png' });
    const shown = await publishListPage(page, 'file-auto-list-hide-when-empty-off', async f => {
      await f.locator('input[name="perc_hide_when_empty"]').uncheck();
    }, none);
    await page.goto(shown);
    await expect(page.locator('.perc-file-auto-list'), 'with the option off the empty list is in the page').toHaveCount(1, { timeout: 30_000 });
    await expect(page.locator('.perc-file-auto-list'), 'and has no hidden attribute').not.toHaveAttribute('hidden', /.*/);
  });
});
