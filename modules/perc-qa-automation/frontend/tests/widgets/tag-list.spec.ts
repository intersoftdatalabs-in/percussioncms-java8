import { test, expect, Page, FrameLocator } from '@playwright/test';
import { loginAsAdmin, createPage, publishOpenItem, configurePublishing, ensureDeliveryResources, ensureSiteFolder } from '../helpers/cms-ui';
import { placeWidget, saveLayout, openContentTab, openWidgetContentEditor, openWidgetConfigure, confirmWidgetConfigure } from '../helpers/widgets-ui';
import { BLOG_INDEX_TEMPLATE, ensureTemplateWithWidget } from '../helpers/blog-ui';
import { DTS_URL, DTS_ADMIN_URL, TEST_SITE, TEST_FOLDER, TEST_TEMPLATE } from '../helpers/qa-env';

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
async function publishTagListPage(page: Page, shot: string, configure?: (form: ReturnType<Page['locator']>) => Promise<void>,
                                 criteria?: (frame: FrameLocator) => Promise<void>): Promise<string> {
  const host = await createPage(page, `Tag List Host ${shot.replace(/^tag-list-/, '')} ${run} ${Date.now().toString().slice(-4)}`);
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
  if (criteria) await criteria(frame);
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

const exactRe = (value: string) => new RegExp(`^${value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}$`);
const EMPTY_FOLDER = 'qa-tag-empty';

test.describe.serial('Tag List', () => {
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

  /** The date boxes are read only: opens the calendar of the box and picks the 15th of the month `months` away from this month (-1 = last month, 1 = next month). */
  const pickDate = (box: '#display_start_date' | '#display_end_date', months: number) => async (f: FrameLocator) => {
    await f.locator(box).locator('xpath=following-sibling::*[1]').click({ timeout: 15_000 });
    const cal = f.locator('#ui-datepicker-div');
    await expect(cal, 'the calendar opens').toBeVisible({ timeout: 10_000 });
    for (let i = 0; i < Math.abs(months); i++) await cal.locator(months > 0 ? '.ui-datepicker-next' : '.ui-datepicker-prev').click();
    await cal.locator('td a.ui-state-default', { hasText: /^15$/ }).first().click();
    await expect(f.locator(box), 'the date is in the box').not.toHaveValue('');
  };
  const both = (...steps: ((f: FrameLocator) => Promise<void>)[]) => async (f: FrameLocator) => { for (const st of steps) await st(f); };
  const noTags = async (page: Page, url: string) => {
    const [response] = await Promise.all([
      page.waitForResponse(
        r => r.url().includes('/perc-metadata-services/metadata/tags/get') && r.request().method() === 'POST',
        { timeout: 60_000 },
      ),
      page.goto(url),
    ]);
    await expect(list(page), 'the list is on the published page').toBeAttached({ timeout: 60_000 });
    expect(response.ok(), 'the DTS tag query succeeded').toBeTruthy();
    const payload = await response.json();
    expect(payload.properties, 'DTS returned no tags').toEqual([]);
    await expect(entries(page), 'no tag is listed').toHaveCount(0);
  };

  test('Tag List: "First published on or after" and "First published before" limit the tags to the pages of that period', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const future = await publishTagListPage(page, 'tag-list-start-date-next-month', undefined, pickDate('#display_start_date', 1));
    await noTags(page, future);
    await page.screenshot({ path: 'screenshots/tag-list-published-page-start-date-next-month-no-tags.png' });
    const past = await publishTagListPage(page, 'tag-list-period-around-today', undefined, both(pickDate('#display_start_date', -1), pickDate('#display_end_date', 1)));
    await open(page, past);
    await expect(entries(page), 'pages created today are inside the period').toHaveText([`${tag('alpha')} (1)`, `${tag('beta')} (3)`, `${tag('gamma')} (1)`]);
    const before = await publishTagListPage(page, 'tag-list-end-date-last-month', undefined, pickDate('#display_end_date', -1));
    await noTags(page, before);
    await page.screenshot({ path: 'screenshots/tag-list-published-page-end-date-last-month-no-tags.png' });
  });

  test('Tag List: the page templates chosen in the content editor limit the tags to pages of those templates', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    await ensureTemplateWithWidget(page, BLOG_INDEX_TEMPLATE, 'percBlogIndexPage');
    const other = await publishTagListPage(page, 'tag-list-template-blog-index', undefined, async f => {
      await f.locator('.perc-pagetemplates-entry', { hasText: exactRe(BLOG_INDEX_TEMPLATE) }).locator('input').check({ timeout: 15_000 });
    });
    await noTags(page, other);
    await page.screenshot({ path: 'screenshots/tag-list-published-page-template-without-matching-pages.png' });
    const base = await publishTagListPage(page, 'tag-list-template-base', undefined, async f => {
      await f.locator('.perc-pagetemplates-entry', { hasText: exactRe(TEST_TEMPLATE) }).locator('input').check({ timeout: 15_000 });
    });
    await open(page, base);
    await expect(entries(page), `the pages use the ${TEST_TEMPLATE} template`).toHaveText([`${tag('alpha')} (1)`, `${tag('beta')} (3)`, `${tag('gamma')} (1)`]);
  });

  test('Tag List: the website location chosen in the content editor limits the tags to the pages of that folder', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    await ensureSiteFolder(page, EMPTY_FOLDER);
    const choose = (folder: string) => async (f: FrameLocator) => {
      const tree = f.locator('#perc-folder-selector');
      await tree.locator('.fancytree-expander').first().click({ timeout: 15_000 });           // Sites
      await tree.locator('.fancytree-title', { hasText: exactRe(TEST_SITE) }).first().waitFor({ timeout: 15_000 });
      await tree.locator('.fancytree-node', { hasText: exactRe(TEST_SITE) }).first().locator('.fancytree-expander').click({ timeout: 15_000 });
      await tree.locator('.fancytree-title', { hasText: exactRe(folder) }).first().click({ timeout: 15_000 });
    };
    const empty = await publishTagListPage(page, 'tag-list-location-other-folder', undefined, choose(EMPTY_FOLDER));
    await noTags(page, empty);
    await page.screenshot({ path: 'screenshots/tag-list-published-page-location-folder-without-matching-pages.png' });
    const there = await publishTagListPage(page, 'tag-list-location-widgets-folder', undefined, choose(TEST_FOLDER));
    await open(page, there);
    await expect(entries(page), `the pages are in the ${TEST_FOLDER} folder`).toHaveText([`${tag('alpha')} (1)`, `${tag('beta')} (3)`, `${tag('gamma')} (1)`]);
  });

  test('Tag List: each tag links to the results page chosen in the content editor, with the tag as the filter', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const resultsPath = `/${TEST_FOLDER}/qa-tag-results.html`;
    const url = await publishTagListPage(page, 'tag-list-results-page', undefined, async f => {
      await f.locator('#display_tag_page_result').fill(resultsPath);
      await f.locator('#display_tag_page_result').blur();
    });
    await open(page, url);
    const alpha = entries(page).filter({ hasText: tag('alpha') });
    const href = (await alpha.getAttribute('href'))!;
    const u = new URL(href, page.url());
    expect(u.pathname, 'the link goes to the results page').toBe(resultsPath);
    expect(u.searchParams.get('filter'), 'the tag is the filter').toBe(tag('alpha'));
    expect(u.searchParams.get('query') ?? '', 'the query of the list is passed on with the tag').toContain(`perc:tags = '${tag('alpha')}'`);
    await page.screenshot({ path: 'screenshots/tag-list-published-page-tags-link-to-results-page.png' });
  });
});
