import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';
import { test, expect, Page, Locator } from '@playwright/test';
import { loginAsAdmin, configurePublishing, ensureDeliveryResources, openPageByPath, publishOpenItem } from '../helpers/cms-ui';
import { ensureBlogInfrastructure, ensureBlog, createAndPublishPost, publishBlogIndex, BLOG_FOLDER, BLOG_INDEX_TEMPLATE } from '../helpers/blog-ui';
import { openWidgetConfigure, confirmWidgetConfigure, openContentTab, widgetOnPage, editorFrame } from '../helpers/widgets-ui';
import { DTS_URL, DTS_ADMIN_URL, TEST_SITE } from '../helpers/qa-env';

/**
 * Blog List Widget: sits on the blog INDEX page and lists the published posts of the blog. The list is built in the browser from the
 * DTS blog service, so the proof is the PUBLISHED index page served by the DTS: the three posts created here are listed, newest first,
 * each with its byline and a link that opens that post.
 */
test.describe('Blog List Widget', () => {
  test.setTimeout(600_000);

  test('Blog List Widget: published blog index lists the posts with their bylines, newest first, and each links to its post', async ({ page }) => {
    const run = Date.now().toString().slice(-7);
    const posts = ['First', 'Second', 'Third'].map(n => ({ title: `QA List Post ${n} ${run}`, byline: `QA Author ${n} ${run}`, body: `Body of the ${n} list post ${run}.`, name: '' }));

    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    await ensureDeliveryResources(page);
    await ensureBlogInfrastructure(page);
    for (const p of posts) p.name = await createAndPublishPost(page, p.title, p.byline, p.body);
    await configureBlogList(page, 'blog-list-defaults');   // the option scenarios change the template: start from the default options, then publish the index

    await page.goto(`${DTS_URL}/${BLOG_FOLDER}/index.html`);
    for (const p of posts) await expect(page.getByText(p.title, { exact: false }).first(), `"${p.title}" in the published list`).toBeVisible({ timeout: 60_000 });
    for (const p of posts) await expect(page.getByText(p.byline).first(), `byline of "${p.title}"`).toBeVisible();

    // newest first: the post created last is listed above the one created before it
    const y = async (t: string) => (await page.getByText(t, { exact: false }).first().boundingBox())!.y;
    expect(await y(posts[2].title), 'third post above second').toBeLessThan(await y(posts[1].title));
    expect(await y(posts[1].title), 'second post above first').toBeLessThan(await y(posts[0].title));
    await page.screenshot({ path: 'screenshots/blog-list-widget-published-index-lists-three-posts-newest-first.png', fullPage: true });

    // the entry links to its post
    await page.locator(`a[href*="${posts[0].name}"]`).first().click();
    await expect(page.getByText(posts[0].body)).toBeVisible({ timeout: 30_000 });
    await page.screenshot({ path: 'screenshots/blog-list-widget-link-opens-the-post-page.png' });
  });
});

/**
 * The options of the Blog List widget are set on the widget in the blog INDEX TEMPLATE (Design > template > Layout > Configure), the index page is
 * published again, and the PUBLISHED index served by the DTS is checked. Every scenario resets the widget to defaults before applying its options, so they do not depend on each other.
 */
process.env.QA_BLOGLIST_RUN ??= Date.now().toString().slice(-7);   // shared with the workers Playwright restarts after a failed scenario
const runKey = process.env.QA_BLOGLIST_RUN;
const postFor = (n: string) => ({ title: `QA Opt Post ${n} ${runKey}`, byline: `QA Opt Author ${n} ${runKey}`, body: `Body of the ${n} option post ${runKey}.` });
const POSTS = ['Alpha', 'Bravo', 'Charlie'];
const HIDE_FIELDS = ['title', 'summary', 'blog_date', 'byline', 'comments'];
const created = path.join(os.tmpdir(), `qa-bloglist-posts-${runKey}.txt`);

/** The default options of the widget (the other scenarios change them on the shared template). */
async function setDefaults(f: Locator) {
  await f.locator('select[name="titleFormat"]').selectOption('h2', { timeout: 10_000 });
  await f.locator('select[name="sortby"]').selectOption('rx:sys_contentpostdate', { timeout: 10_000 });
  await f.locator('select[name="sortDirection"]').selectOption('desc', { timeout: 10_000 });
  await f.locator('select[name="locale"]').selectOption('en-US', { timeout: 10_000 });
  await f.locator('input[name="blogPostDateTimeFormat"]').fill('EEE MMM d, yyyy hh:mm a');
  await f.locator('input[name="blogPostBylineText"]').fill('by');
  await f.locator('input[name="blogPostCommentsText"]').fill('Comments');
  await f.locator('input[name="blogPostReadMoreText"]').fill('more...');
  await f.locator('input[name="blogPostMaxResults"]').fill('10');
  await f.locator('input[name="totalMaxResults"]').fill('500');
  await f.locator('input[name="autoListDivId"]').fill('');
  await f.locator('input[name="listAriaLabel"]').fill('');
  await f.locator('input[name="listAriaRole"]').fill('');
  await f.locator('input[name="enable_rss_icon"]').uncheck();
  await f.locator('input[name="enableCalenderIcon"]').uncheck();
  await f.locator('input[name="perc_hidefield_tags"]').uncheck();
  await f.locator('input[name="perc_tag_label"]').fill('Tags:');
  await f.locator('input[name="perc_hidefield_categories"]').uncheck();
  await f.locator('input[name="perc_category_label"]').fill('Categories:');
  await f.locator('input[name="blogListHeaderText"]').fill('Results for');
  await f.locator('input[name="pagingPagesText"]').fill('pages');
  await f.locator('input[name="resultEntriesTextPlural"]').fill('entries');
  await f.locator('input[name="resultEntriesTextSingular"]').fill('entry');
  await f.locator('select[name="rssIconPosition"]').selectOption('perc-list-top', { timeout: 10_000 });
  await f.locator('input[name="rssLinkText"]').fill('RSS Link');
  await f.locator('input[name="perc_hide_when_empty"]').uncheck();
  for (const n of HIDE_FIELDS) await f.locator(`input[name="perc_hidefield_${n}"]`).uncheck();
}

async function configureBlogList(page: Page, shot: string, set?: (form: Locator) => Promise<void>) {
  await page.goto('/cm/app/?view=design');
  await page.getByText(TEST_SITE, { exact: true }).first().dblclick({ timeout: 60_000 });
  await page.locator('.perc-template-item').filter({ hasText: BLOG_INDEX_TEMPLATE }).locator('button.perc-edit').click({ timeout: 60_000 });
  await page.getByText('Layout', { exact: true }).first().click();
  const form = await openWidgetConfigure(page, 'percBlogIndexPage');
  await form.getByText('Properties', { exact: true }).click();   // the options are in the collapsed Properties section
  await expect(form.locator('select[name="titleFormat"]')).toBeVisible({ timeout: 10_000 });
  await setDefaults(form);
  if (set) await set(form);
  await page.screenshot({ path: `screenshots/${shot}-configure-dialog.png` });
  await confirmWidgetConfigure(page);
  await page.getByRole('button', { name: 'Save' }).click();
  await expect(page.getByText(/Processing\. Please wait/)).toBeHidden({ timeout: 60_000 });
  await publishBlogIndex(page);
}


const postFiles = (): Record<string, string> => JSON.parse(fs.readFileSync(created, 'utf8'));
const CATEGORY = 'QA Cat Alpha Child';
const TAGS = ['qalisttagone', 'qalisttagtwo'];

/** Actions > Edit Metadata > Tags and Categories of the open page: opens the section and returns the metadata frame. */
async function openTagCategorySection(page: Page) {
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
  return frame;
}

/** Administration > Categories: makes sure the category QA Cat Alpha with its child exists. */
async function ensureCategories(page: Page) {
  await page.goto('/cm/app/?view=workflow');
  const tab = page.locator('#tabs a, .ui-tabs-nav a').filter({ hasText: /^Categories$/ }).first();
  await expect(tab).toBeVisible({ timeout: 60_000 });
  await tab.click();
  const tree = page.locator('#perc-category-tree');
  await expect(page.locator('#perc-categories-add-category-button')).toBeVisible({ timeout: 30_000 });
  const typeName = async (name: string) => {
    await page.locator('#perc-category-name-field').click();
    await page.keyboard.press('Control+A');
    await page.keyboard.type(name, { delay: 40 });
    await page.locator('#perc-category-save').click();
  };
  if (!((await tree.textContent()) ?? '').includes('QA Cat Alpha')) {
    await page.locator('#perc-categories-add-category-button').click();
    await typeName('QA Cat Alpha');
    await expect(tree).toContainText('QA Cat Alpha', { timeout: 30_000 });
  }
  const rootNode = tree.locator('.fancytree-node').filter({ has: page.locator('.fancytree-title', { hasText: /^QA Cat Alpha$/ }) }).first();
  if ((await rootNode.locator('.fancytree-expander').isVisible()) && !((await rootNode.getAttribute('class')) ?? '').includes('fancytree-expanded')) {
    await rootNode.locator('.fancytree-expander').click();
    await page.waitForTimeout(1500);
  }
  if (!((await tree.textContent()) ?? '').includes(CATEGORY)) {
    await tree.locator('.fancytree-title', { hasText: /^QA Cat Alpha$/ }).click();
    await page.locator('#perc-categories-add-child-category-button').click();
    await typeName(CATEGORY);
    await expect.poll(async () => (await tree.textContent()) ?? '', { timeout: 30_000 }).toContain(CATEGORY);
  }
}

/** Gives the first post two tags and one category and publishes it again (once per run). */
async function ensureTagged(page: Page) {
  const flag = path.join(os.tmpdir(), `qa-bloglist-tagged-${runKey}.txt`);
  if (fs.existsSync(flag)) return;
  await ensureCategories(page);
  await openPageByPath(page, [BLOG_FOLDER], `${postFiles().Alpha}.html`);
  const frame = await openTagCategorySection(page);
  await frame.locator('#page_tags-display').fill(TAGS.join(', '));
  await page.keyboard.press('Escape');
  await frame.locator(`#page_categories_tree-tree span.fancytree-node:has(> span.fancytree-title:text-is("${CATEGORY}")) > span.fancytree-checkbox`).click();
  const dialog = page.locator('.ui-dialog:visible', { hasText: 'Edit Metadata' });
  await dialog.getByRole('button', { name: 'Save' }).click();
  await expect(dialog).toBeHidden({ timeout: 60_000 });
  await openContentTab(page);
  await publishOpenItem(page);
  fs.writeFileSync(flag, 'ok');
}

/** Turns the RSS feed on in the content of the Blog List widget on the blog index page (once per run); the icon needs a feed name. */
async function ensureFeed(page: Page) {
  const flag = path.join(os.tmpdir(), `qa-bloglist-feed-${runKey}.txt`);
  if (fs.existsSync(flag)) return;
  await openPageByPath(page, [BLOG_FOLDER], 'index.html');
  await openContentTab(page);
  await widgetOnPage(page, 'percBlogIndexPage').click();
  await editorFrame(page).locator('img[title="Edit local content"]').first().click();
  const frame = page.frameLocator('iframe#edit-widget-content-frame');
  const enable = frame.locator('#perc-enable-feed');
  await expect(enable).toBeVisible({ timeout: 30_000 });
  if (!(await enable.isChecked())) await enable.check();
  await frame.locator('#perc-feed-title').fill(`QA Blog Feed ${runKey}`);
  await frame.locator('#perc-feed-name').fill(`qablogfeed${runKey}`);
  await frame.locator('#perc-feed-description').fill('Feed of the QA blog');
  const dialog = page.locator('.ui-dialog:visible', { hasText: 'Edit Widget Content' });
  await dialog.getByRole('button', { name: 'Save' }).click();
  await expect(dialog).toBeHidden({ timeout: 30_000 });
  fs.writeFileSync(flag, 'ok');
}

/** Gives the second post a summary of its own with the "More Link" marker (the summary is generated by default) and publishes the post again (once per run). */
async function ensureSummary(page: Page) {
  const flag = path.join(os.tmpdir(), `qa-bloglist-summary-${runKey}.txt`);
  if (fs.existsSync(flag)) return;
  await openPageByPath(page, [BLOG_FOLDER], `${postFiles().Bravo}.html`);
  await expect(async () => {
    await page.getByText('Actions', { exact: true }).first().click();
    await page.getByText('Edit Metadata', { exact: true }).first().click({ timeout: 3_000 });
    await expect(page.locator('iframe#edit-page-metadata-frame')).toBeVisible({ timeout: 5_000 });
  }).toPass({ timeout: 60_000 });
  const frame = page.frameLocator('iframe#edit-page-metadata-frame');
  const label = frame.locator('.perc-section-label[group="perc-pageSum-container"]');
  await expect(async () => {
    if (!(await label.isVisible())) await frame.locator('#perc-content-edit-metadata-link').click({ timeout: 3_000 });
    await expect(label).toBeVisible({ timeout: 3_000 });
  }).toPass({ timeout: 60_000 });
  const auto = frame.locator('#perc-content-edit-auto_generate_summary');
  await expect(async () => {
    if (!(await auto.isVisible())) await label.click({ timeout: 3_000 });
    await expect(auto).toBeVisible({ timeout: 3_000 });
  }).toPass({ timeout: 30_000 });
  await auto.uncheck();
  await expect(frame.locator('.tox-tinymce:visible'), 'the summary editor appears when the summary is not generated').toBeVisible({ timeout: 15_000 });
  await frame.frameLocator('.tox-tinymce:visible .tox-edit-area__iframe').locator('body').click();
  await page.keyboard.type(`Own summary of the Bravo post ${runKey} `);
  await frame.locator('.tox-tinymce:visible').getByRole('button', { name: 'More Link' }).click();
  const dialog = page.locator('.ui-dialog:visible', { hasText: 'Edit Metadata' });
  await dialog.getByRole('button', { name: 'Save' }).click();
  await expect(dialog).toBeHidden({ timeout: 60_000 });
  await openContentTab(page);
  await publishOpenItem(page);
  fs.writeFileSync(flag, 'ok');
}

const entry = (page: Page, title: string) => page.locator('li.ui-perc-list-element', { hasText: title });

test.describe('Blog List Widget options', () => {
  test.setTimeout(300_000);

  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    if (!fs.existsSync(created)) {
      await ensureDeliveryResources(page);
      await ensureBlogInfrastructure(page);
      const made: Record<string, string> = {};
      for (const n of POSTS) { const p = postFor(n); made[n] = await createAndPublishPost(page, p.title, p.byline, p.body); }
      fs.writeFileSync(created, JSON.stringify(made));
    }
  });

  test('Blog List: title element, texts, date format and the show/hide of date, byline, summary and comments from Configure are applied', async ({ page }) => {
    await configureBlogList(page, 'blog-list-texts', async f => {
      await f.locator('select[name="titleFormat"]').selectOption('h3', { timeout: 10_000 });
      await f.locator('input[name="blogPostDateTimeFormat"]').fill('yyyy-MM-dd');
      await f.locator('input[name="blogPostBylineText"]').fill('written by');
      await f.locator('input[name="blogPostCommentsText"]').fill('Remarks');
      await f.locator('input[name="blogPostReadMoreText"]').fill('Read on');
    });
    await page.goto(`${DTS_URL}/${BLOG_FOLDER}/index.html`);
    const e = entry(page, postFor('Alpha').title);
    await expect(e, 'the post is listed').toBeVisible({ timeout: 60_000 });
    await expect(e.locator('h3.perc-blog-list-title'), 'title is a Heading 3').toHaveCount(1);
    await expect(e.locator('.perc-blog-list-byline-container'), 'byline text').toHaveText(new RegExp(`^written by\\s+${postFor('Alpha').byline}`));
    await expect(e.locator('.perc-blog-list-date-container'), 'date format').toHaveText(/^\d{4}-\d{2}-\d{2}/);
    await expect(e.locator('.perc-blog-list-comment-container a'), 'comments text').toHaveText('Remarks');
    await page.screenshot({ path: 'screenshots/blog-list-published-index-texts-and-date-format.png', fullPage: true });
  });

  test('Blog List: hiding the title, summary, date, byline and comments removes them from the published list', async ({ page }) => {
    await configureBlogList(page, 'blog-list-hide', async f => {
      for (const n of HIDE_FIELDS) await f.locator(`input[name="perc_hidefield_${n}"]`).check();
    });
    await page.goto(`${DTS_URL}/${BLOG_FOLDER}/index.html`);
    await expect(page.locator('ol.perc-blog-list.perc-list-main li.ui-perc-list-element').first(), 'the list is built (its entries are empty, so they are not visible)').toBeAttached({ timeout: 60_000 });
    for (const c of ['.perc-blog-list-title', '.perc-blog-list-summary-container', '.perc-blog-list-date-container', '.perc-blog-list-byline-container', '.perc-blog-list-comment-container'])
      await expect(page.locator(`ol.perc-blog-list.perc-list-main ${c}`), `${c} is hidden`).toHaveCount(0);
    await page.screenshot({ path: 'screenshots/blog-list-published-index-fields-hidden.png', fullPage: true });
  });

  test('Blog List: sorted by page title ascending with a page size of 2, the first page shows two entries', async ({ page }) => {
    await configureBlogList(page, 'blog-list-sort-paging', async f => {
      await f.locator('select[name="sortby"]').selectOption('rx:page_title', { timeout: 10_000 });
      await f.locator('select[name="sortDirection"]').selectOption('asc', { timeout: 10_000 });
      await f.locator('input[name="blogPostMaxResults"]').fill('2');
    });
    await page.goto(`${DTS_URL}/${BLOG_FOLDER}/index.html`);
    await expect(page.locator('.perc-blog-list-container[aria-busy="false"]'), 'DTS blog list finished loading').toHaveCount(1, { timeout: 60_000 });
    const items = page.locator('ol.perc-blog-list.perc-list-main > li.ui-perc-list-element:visible');
    await expect(items, 'two entries on the first page').toHaveCount(2, { timeout: 60_000 });
    const titles = (await items.locator('.perc-blog-list-title').allInnerTexts()).map(t => t.trim());
    expect(titles, 'two titles rendered on the first page').toHaveLength(2);
    expect(titles[0].toLowerCase() <= titles[1].toLowerCase(), `titles sorted ascending: ${titles.join(' | ')}`).toBe(true);
    const pager = page.locator('.perc-pagination-container');
    await expect(pager, 'pagination controls are shown for more than 2 posts').toBeVisible({ timeout: 30_000 });
    await expect(pager.locator('.perc-page-count-current'), 'starts on page 1').toContainText(/^1\b/);
    await expect(pager.locator('a.perc-second'), 'page 2 link is offered').toBeVisible();
    await page.screenshot({ path: 'screenshots/blog-list-published-index-two-per-page.png', fullPage: true });
  });

  test('Blog List: list id, aria label and role from Configure are written', async ({ page }) => {
    await configureBlogList(page, 'blog-list-aria-rss', async f => {
      await f.locator('input[name="autoListDivId"]').fill(`qalist${runKey}`);
      await f.locator('input[name="listAriaLabel"]').fill('QA blog posts');
      await f.locator('input[name="listAriaRole"]').fill('feed');
    });
    await page.goto(`${DTS_URL}/${BLOG_FOLDER}/index.html`);
    const box = page.locator(`#qalist${runKey}`);
    await expect(box, 'list id').toHaveCount(1, { timeout: 60_000 });
    await expect(box, 'aria label').toHaveAttribute('aria-label', 'QA blog posts');
    await expect(box, 'role').toHaveAttribute('role', 'feed');
    await page.screenshot({ path: 'screenshots/blog-list-published-index-aria-and-rss.png', fullPage: true });
  });

  test('Blog List: calendar icon, French locale date and a total maximum of 2 from Configure are applied', async ({ page }) => {
    await configureBlogList(page, 'blog-list-calendar-locale-total', async f => {
      await f.locator('input[name="enableCalenderIcon"]').check();
      await f.locator('select[name="locale"]').selectOption('fr-FR', { timeout: 10_000 });
      await f.locator('input[name="blogPostDateTimeFormat"]').fill('EEEE d MMMM yyyy');
      await f.locator('input[name="totalMaxResults"]').fill('2');
    });
    await page.goto(`${DTS_URL}/${BLOG_FOLDER}/index.html`);
    const items = page.locator('ol.perc-blog-list.perc-list-main > li.ui-perc-list-element');
    await expect(items, 'the total maximum limits the list to 2 entries').toHaveCount(2, { timeout: 60_000 });
    await expect(items.first().locator('.perc-blog-list-calicon'), 'calendar icon is shown').toBeVisible();
    await expect(items.first().locator('.perc-blog-list-calicon-day'), 'calendar icon day').toHaveText(/^\d{1,2}$/);
    const date = await items.first().locator('.perc-blog-list-date-container').innerText();
    expect(date, 'date written in French (day name and month name)').toMatch(/(lundi|mardi|mercredi|jeudi|vendredi|samedi|dimanche) \d{1,2} \p{L}+ \d{4}/u);
    await page.screenshot({ path: 'screenshots/blog-list-published-index-calendar-icon-french-total-2.png', fullPage: true });
  });

  test('Blog List: tags and categories are shown with the label texts from Configure', async ({ page }) => {
    await ensureTagged(page);
    await configureBlogList(page, 'blog-list-tags-categories', async f => {
      await f.locator('input[name="perc_tag_label"]').fill('Keywords:');
      await f.locator('input[name="perc_category_label"]').fill('Topics:');
    });
    await page.goto(`${DTS_URL}/${BLOG_FOLDER}/index.html`);
    const e = entry(page, postFor('Alpha').title);
    await expect(e, 'the tagged post is listed').toBeVisible({ timeout: 60_000 });
    await expect(e.locator('.perc-blog-list-tag-container'), 'tag label text').toContainText('Keywords:');
    await expect(e.locator('.perc-blog-list-tag-container'), 'both tags').toContainText(TAGS[0]);
    await expect(e.locator('.perc-blog-list-tag-container'), 'both tags').toContainText(TAGS[1]);
    await expect(e.locator('.perc-blog-list-category-container'), 'category label text').toContainText('Topics:');
    await expect(e.locator('.perc-blog-list-category-container'), 'the category').toContainText(CATEGORY);
    await page.screenshot({ path: 'screenshots/blog-list-published-index-tags-and-categories-with-labels.png', fullPage: true });
  });

  test('Blog List: hiding the tags and the categories removes them from the published list', async ({ page }) => {
    await ensureTagged(page);
    await configureBlogList(page, 'blog-list-tags-categories-hidden', async f => {
      await f.locator('input[name="perc_hidefield_tags"]').check();
      await f.locator('input[name="perc_hidefield_categories"]').check();
    });
    await page.goto(`${DTS_URL}/${BLOG_FOLDER}/index.html`);
    await expect(entry(page, postFor('Alpha').title), 'the post is listed').toBeVisible({ timeout: 60_000 });
    await expect(page.locator('ol.perc-blog-list.perc-list-main .perc-blog-list-tag-container'), 'no tags').toHaveCount(0);
    await expect(page.locator('ol.perc-blog-list.perc-list-main .perc-blog-list-category-container'), 'no categories').toHaveCount(0);
    await page.screenshot({ path: 'screenshots/blog-list-published-index-tags-and-categories-hidden.png', fullPage: true });
  });

  test('Blog List: sorted by page title the entries are in title order, ascending and descending', async ({ page }) => {
    const titles = async () => (await page.locator('ol.perc-blog-list.perc-list-main h2.perc-blog-list-title').allInnerTexts())
      .filter(t => t.includes(runKey)).map(t => POSTS.find(n => t.includes(`Post ${n} `)));
    await configureBlogList(page, 'blog-list-sort-title-asc', async f => {
      await f.locator('input[name="blogPostMaxResults"]').fill('500');   // this run's posts must not fall off the first page
      await f.locator('select[name="sortby"]').selectOption('rx:page_title', { timeout: 10_000 });
      await f.locator('select[name="sortDirection"]').selectOption('asc', { timeout: 10_000 });
    });
    await page.goto(`${DTS_URL}/${BLOG_FOLDER}/index.html`);
    await expect(page.locator('.perc-blog-list-container[aria-busy="false"]'), 'DTS blog list finished loading').toHaveCount(1, { timeout: 60_000 });
    await expect(entry(page, postFor('Alpha').title)).toBeVisible({ timeout: 60_000 });
    expect(await titles(), 'ascending by title').toEqual(['Alpha', 'Bravo', 'Charlie']);
    await page.screenshot({ path: 'screenshots/blog-list-published-index-sorted-by-title-ascending.png', fullPage: true });
    await configureBlogList(page, 'blog-list-sort-title-desc', async f => {
      await f.locator('input[name="blogPostMaxResults"]').fill('500');
      await f.locator('select[name="sortby"]').selectOption('rx:page_title', { timeout: 10_000 });
      await f.locator('select[name="sortDirection"]').selectOption('desc', { timeout: 10_000 });
    });
    await page.goto(`${DTS_URL}/${BLOG_FOLDER}/index.html`);
    await expect(page.locator('.perc-blog-list-container[aria-busy="false"]'), 'DTS blog list finished loading').toHaveCount(1, { timeout: 60_000 });
    await expect(entry(page, postFor('Alpha').title)).toBeVisible({ timeout: 60_000 });
    expect(await titles(), 'descending by title').toEqual(['Charlie', 'Bravo', 'Alpha']);
    await page.screenshot({ path: 'screenshots/blog-list-published-index-sorted-by-title-descending.png', fullPage: true });
  });

  test('Blog List: header text, entries text and paging text from Configure are written on a filtered list', async ({ page }) => {
    await configureBlogList(page, 'blog-list-header-paging-texts', async f => {
      await f.locator('input[name="blogListHeaderText"]').fill('Matching posts for');
      await f.locator('input[name="resultEntriesTextPlural"]').fill('posts found');
      await f.locator('input[name="pagingPagesText"]').fill('qapages');
      await f.locator('input[name="blogPostMaxResults"]').fill('2');
    });
    const query = encodeURIComponent(JSON.stringify({ criteria: [] }));
    await page.goto(`${DTS_URL}/${BLOG_FOLDER}/index.html?filter=QAFilter&query=${query}`);
    await expect(page.locator('.perc-bloglist-result-title'), 'header text with the filter').toHaveText('Matching posts for QAFilter', { timeout: 60_000 });
    await expect(page.locator('.perc-bloglist-result-count'), 'entries text').toHaveText(/^\d+ posts found$/);
    await expect(page.locator('.perc-pagination-container'), 'paging text').toContainText('qapages');
    await page.screenshot({ path: 'screenshots/blog-list-published-index-header-entries-and-paging-texts.png', fullPage: true });
  });

  test('Blog List: the RSS icon is shown at the top or the bottom of the list with the link text from Configure', async ({ page }) => {
    await ensureFeed(page);
    await configureBlogList(page, 'blog-list-rss-icon-top', async f => {
      await f.locator('input[name="enable_rss_icon"]').check();
      await f.locator('select[name="rssIconPosition"]').selectOption('perc-list-top', { timeout: 10_000 });
      await f.locator('input[name="rssLinkText"]').fill('QA feed top');
    });
    await page.goto(`${DTS_URL}/${BLOG_FOLDER}/index.html`);
    await expect(page.locator('.perc-rss-icon-top a.perc-rss-icon'), 'RSS icon at the top with its link text').toHaveAttribute('aria-label', 'QA feed top', { timeout: 60_000 });
    await expect(page.locator('.perc-rss-icon-top a.perc-rss-icon'), 'the link points to the feed').toHaveAttribute('href', new RegExp(`/feeds/rss/.+/qablogfeed${runKey}/`));
    await expect(page.locator('.perc-rss-icon-bottom'), 'no icon at the bottom').toHaveCount(0);
    await page.screenshot({ path: 'screenshots/blog-list-published-index-rss-icon-top.png', fullPage: true });
    await configureBlogList(page, 'blog-list-rss-icon-bottom', async f => {
      await f.locator('input[name="enable_rss_icon"]').check();
      await f.locator('select[name="rssIconPosition"]').selectOption('perc-list-bottom', { timeout: 10_000 });
      await f.locator('input[name="rssLinkText"]').fill('QA feed bottom');
    });
    await page.goto(`${DTS_URL}/${BLOG_FOLDER}/index.html`);
    await expect(page.locator('.perc-rss-icon-bottom a.perc-rss-icon'), 'RSS icon at the bottom with its link text').toHaveAttribute('aria-label', 'QA feed bottom', { timeout: 60_000 });
    await expect(page.locator('.perc-rss-icon-top'), 'no icon at the top').toHaveCount(0);
    await page.screenshot({ path: 'screenshots/blog-list-published-index-rss-icon-bottom.png', fullPage: true });
  });

  test('Blog List: the read more link text from Configure is written after the summary of a post', async ({ page }) => {
    await ensureSummary(page);
    await configureBlogList(page, 'blog-list-read-more', async f => {
      await f.locator('input[name="blogPostReadMoreText"]').fill('Read on QA');
    });
    await page.goto(`${DTS_URL}/${BLOG_FOLDER}/index.html`);
    await expect(page.locator('.perc-blog-list-container[aria-busy="false"]'), 'DTS blog list finished loading').toHaveCount(1, { timeout: 60_000 });
    const e = entry(page, postFor('Bravo').title);
    await expect(e, 'the post is listed').toBeVisible({ timeout: 60_000 });
    await expect(e.locator('.perc-blog-list-summary-container'), 'the own summary text').toContainText(`Own summary of the Bravo post ${runKey}`);
    const more = e.locator('.perc-blog-list-summary-container a', { hasText: 'Read on QA' });
    await expect(more, 'read more link with the text from Configure').toHaveCount(1);
    await expect(more, 'the link opens the post').toHaveAttribute('href', new RegExp(`${postFiles().Bravo}\\.html`));
    await page.screenshot({ path: 'screenshots/blog-list-published-index-read-more-link.png', fullPage: true });
  });

  test('Blog List: the singular entries text from Configure is used when the filtered list has one entry', async ({ page }) => {
    await configureBlogList(page, 'blog-list-entries-singular', async f => {
      await f.locator('input[name="resultEntriesTextSingular"]').fill('QA matching post');
      await f.locator('input[name="resultEntriesTextPlural"]').fill('QA matching posts');
    });
    const one = encodeURIComponent(JSON.stringify({ criteria: [`dcterms:title LIKE '%${postFor('Alpha').title}%'`] }));
    await page.goto(`${DTS_URL}/${BLOG_FOLDER}/index.html?filter=Alpha&query=${one}`);
    await expect(page.locator('.perc-bloglist-result-count'), 'singular text for one entry').toHaveText('1 QA matching post', { timeout: 60_000 });
    const all = encodeURIComponent(JSON.stringify({ criteria: [`dcterms:title LIKE '%${runKey}%'`] }));
    await page.goto(`${DTS_URL}/${BLOG_FOLDER}/index.html?filter=All&query=${all}`);
    await expect(page.locator('.perc-bloglist-result-count'), 'plural text for three entries').toHaveText('3 QA matching posts', { timeout: 60_000 });
    await page.screenshot({ path: 'screenshots/blog-list-published-index-singular-and-plural-entries-text.png', fullPage: true });
  });

  test('Blog List: "Hide blog list when empty" hides the list of a blog without posts and shows it when the option is off', async ({ page }) => {
    const EMPTY_TITLE = 'QA Empty Blog';
    const EMPTY_FOLDER = 'qa-empty-blog';
    await ensureBlog(page, EMPTY_TITLE, EMPTY_FOLDER);
    const publishEmptyIndex = async () => {
      await openPageByPath(page, [EMPTY_FOLDER], 'index.html');
      await openContentTab(page);
      await publishOpenItem(page);
    };
    const container = page.locator('.perc-blog-list-container');
    const noPost = page.locator('.perc-no-post');   // the empty list's own message
    await configureBlogList(page, 'blog-list-hide-when-empty-on', async f => { await f.locator('input[name="perc_hide_when_empty"]').check(); });
    await publishEmptyIndex();
    await page.goto(`${DTS_URL}/${EMPTY_FOLDER}/index.html`);
    await page.waitForLoadState('networkidle');
    await expect(page.locator('.bloglist-sample-content'), 'not replaced by the sample content placeholder').toHaveCount(0);
    await expect(container.locator('ol.perc-blog-list.perc-list-main > li.ui-perc-list-element'), 'no posts in this blog').toHaveCount(0);
    await expect(container, 'the empty list container is rendered').toBeAttached();
    await expect(container, 'the empty list container has the hidden attribute').toHaveAttribute('hidden', '');
    await expect(container, 'the empty list is hidden').toBeHidden();
    await expect(noPost, 'with the option on the empty list message is hidden').toBeHidden();
    await page.screenshot({ path: 'screenshots/blog-list-published-empty-blog-list-hidden.png' });
    await configureBlogList(page, 'blog-list-hide-when-empty-off', async f => { await f.locator('input[name="perc_hide_when_empty"]').uncheck(); });
    await publishEmptyIndex();
    await page.goto(`${DTS_URL}/${EMPTY_FOLDER}/index.html`);
    await page.waitForLoadState('networkidle');
    await expect(page.locator('.bloglist-sample-content'), 'not replaced by the sample content placeholder').toHaveCount(0);
    await expect(container, 'with the option off the (empty) list is still in the page').toHaveCount(1);
    await expect(container, 'with the option off the container is not hidden').not.toHaveAttribute('hidden', /.*/);
    await page.screenshot({ path: 'screenshots/blog-list-published-empty-blog-list-shown.png' });
  });
});
