import { test, expect, Page } from '@playwright/test';
import { loginAsAdmin, createPage, publishOpenItem, configurePublishing, ensureDeliveryResources } from '../helpers/cms-ui';
import { placeWidget, saveLayout, openContentTab, openWidgetContentEditor, openWidgetConfigure, confirmWidgetConfigure } from '../helpers/widgets-ui';
import { DTS_URL, DTS_ADMIN_URL, TEST_FOLDER } from '../helpers/qa-env';

/**
 * Tag List (widget "Tags"): lists the tags of the PUBLISHED pages that match the criteria set in the widget's content editor (here: link text
 * contains a token), each with the number of pages that have it. The list is built in the browser from the DTS, so the proof is the published page.
 * Three pages are created, tagged and published first: tag alpha on one page, beta on all three, gamma on one.
 */
process.env.QA_TAGS_RUN ??= Date.now().toString().slice(-7);   // shared with the workers Playwright restarts after a failed scenario
const run = process.env.QA_TAGS_RUN;
const token = `QA TAGS ${run}`;
const tag = (n: string) => `qa${run}${n}`;

/** Actions > Edit Metadata > Tags and Categories: types the tags (comma separated) on the open page and saves. */
async function assignTags(page: Page, tags: string[]) {
  await expect(async () => {
    await page.getByText('Actions', { exact: true }).first().click();
    await page.getByText('Edit Metadata', { exact: true }).first().click({ timeout: 3_000 });
    await expect(page.locator('iframe#edit-page-metadata-frame')).toBeVisible({ timeout: 5_000 });
  }).toPass({ timeout: 60_000 });
  const frame = page.frameLocator('iframe#edit-page-metadata-frame');
  const label = frame.locator('.perc-section-label[group="perc-tagCat-container"]');
  await expect(async () => {
    if (!(await label.isVisible())) await frame.locator('#perc-content-edit-metadata-link').click({ timeout: 3_000 });
    await expect(label).toBeVisible({ timeout: 3_000 });
  }).toPass({ timeout: 60_000 });
  await label.click();
  const box = frame.locator('#page_tags-display');
  await box.fill(tags.join(', '));
  await page.keyboard.press('Escape');
  const dialog = page.locator('.ui-dialog:visible', { hasText: 'Edit Metadata' });
  await dialog.getByRole('button', { name: 'Save' }).click();
  await expect(dialog).toBeHidden({ timeout: 60_000 });
}

/** A new page with a Tag List (optionally configured) whose criteria is: link text contains the token. Published; returns the published URL. */
async function publishTagListPage(page: Page, shot: string, configure?: (form: ReturnType<Page['locator']>) => Promise<void>): Promise<string> {
  const host = await createPage(page, `Tag List Host ${shot.slice(-7)} ${run}`);
  await placeWidget(page, 'percTagList');
  if (configure) {
    const form = await openWidgetConfigure(page, 'percTagList');
    await configure(form);
    await page.screenshot({ path: `screenshots/${shot}-configure-dialog.png` });
    await confirmWidgetConfigure(page);
  }
  await saveLayout(page);
  const { frame, save } = await openWidgetContentEditor(page, 'percTagList');
  await frame.locator('#perc-content-edit-displaytitle').fill(`Tags ${run}`);
  await frame.locator('#display_title_contains').fill(token);
  await page.screenshot({ path: `screenshots/${shot}-content-editor-criteria.png` });
  await save.click();
  await expect(save).toBeHidden({ timeout: 60_000 });
  await openContentTab(page);
  await publishOpenItem(page);
  return `${DTS_URL}/${TEST_FOLDER}/${host}.html`;
}

const list = (page: Page) => page.locator('.perc-tag-list');
const entries = (page: Page) => list(page).locator('li.perc-tag-element a:visible');
async function open(page: Page, url: string) {
  await page.goto(url);
  await expect(entries(page).first(), 'the tags are loaded from the DTS').toBeVisible({ timeout: 60_000 });
}

test.describe('Tag List', () => {
  test.setTimeout(240_000);

  test('Tag List: default options list each tag of the matching pages with the number of pages, in alphabetical order, under the list title', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    await ensureDeliveryResources(page);
    const pages: [string, string[]][] = [['One', [tag('alpha'), tag('beta')]], ['Two', [tag('beta')]], ['Three', [tag('beta'), tag('gamma')]]];
    for (const [n, tags] of pages) {
      await createPage(page, `${token} ${n}`);
      await assignTags(page, tags);
      await openContentTab(page);
      await publishOpenItem(page);
    }
    const url = await publishTagListPage(page, 'tag-list-defaults');

    await open(page, url);
    await expect(list(page).locator('.perc-tag-title'), 'list title').toHaveText(`Tags ${run}`);
    await expect(entries(page), 'alphabetical, each with its page count').toHaveText([`${tag('alpha')} (1)`, `${tag('beta')} (3)`, `${tag('gamma')} (1)`]);
    await expect(list(page).locator('ul.perc-list-main.perc-list-vertical'), 'default display format is a list, one tag under the other').toHaveCount(1);
    await page.screenshot({ path: 'screenshots/tag-list-published-page-default-list.png' });
  });

  test('Tag List: sort method "Count of tags" puts the most used tag first', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishTagListPage(page, 'tag-list-sort-count', async f => {
      await f.locator('select[name="tag_sort_option"]').selectOption('count', { timeout: 10_000 });
    });
    await open(page, url);
    await expect(entries(page).first(), 'the tag on three pages is first').toHaveText(`${tag('beta')} (3)`);
    await page.screenshot({ path: 'screenshots/tag-list-published-page-sorted-by-count.png' });
  });

  test('Tag List: display format "Comma separated" lists the tags in one line', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishTagListPage(page, 'tag-list-comma-separated', async f => {
      await f.locator('select[name="tag_render_option"]').selectOption('commaSeparated', { timeout: 10_000 });
    });
    await open(page, url);
    await expect(list(page).locator('ul.perc-list-main.perc-list-main-inline'), 'comma separated (inline) list').toHaveCount(1);
    await expect(list(page).locator('ul.perc-list-vertical'), 'not the one-under-the-other list').toHaveCount(0);
    await expect(entries(page), 'the separator comma follows each tag but the last').toHaveText([`${tag('alpha')} (1),`, `${tag('beta')} (3),`, `${tag('gamma')} (1)`]);
    await page.screenshot({ path: 'screenshots/tag-list-published-page-comma-separated.png' });
  });
});
