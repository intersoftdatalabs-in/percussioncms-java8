import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';
import { test, expect, Page, Locator } from '@playwright/test';
import { loginAsAdmin, configurePublishing, ensureDeliveryResources } from '../helpers/cms-ui';
import { ensureBlogInfrastructure, createAndPublishPost, publishBlogIndex, BLOG_FOLDER, BLOG_INDEX_TEMPLATE } from '../helpers/blog-ui';
import { openWidgetConfigure, confirmWidgetConfigure } from '../helpers/widgets-ui';
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
    await configureBlogList(page, 'blog-list-defaults', setDefaults);   // the option scenarios change the template: start from the default options, then publish the index

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
 * published again, and the PUBLISHED index served by the DTS is checked. Every scenario sets all the options it checks, so they do not depend on each other.
 */
process.env.QA_BLOGLIST_RUN ??= Date.now().toString().slice(-7);   // shared with the workers Playwright restarts after a failed scenario
const runKey = process.env.QA_BLOGLIST_RUN;
const postFor = (n: string) => ({ title: `QA Opt Post ${n} ${runKey}`, byline: `QA Opt Author ${n} ${runKey}`, body: `Body of the ${n} option post ${runKey}.` });
const POSTS = ['Alpha', 'Bravo', 'Charlie'];
const HIDE_FIELDS = ['title', 'summary', 'blog_date', 'byline', 'comments'];

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
  for (const n of HIDE_FIELDS) await f.locator(`input[name="perc_hidefield_${n}"]`).uncheck();
}

async function configureBlogList(page: Page, shot: string, set: (form: Locator) => Promise<void>) {
  await page.goto('/cm/app/?view=design');
  await page.getByText(TEST_SITE, { exact: true }).first().dblclick({ timeout: 60_000 });
  await page.locator('.perc-template-item').filter({ hasText: BLOG_INDEX_TEMPLATE }).locator('button.perc-edit').click({ timeout: 60_000 });
  await page.getByText('Layout', { exact: true }).first().click();
  const form = await openWidgetConfigure(page, 'percBlogIndexPage');
  await form.getByText('Properties', { exact: true }).click();   // the options are in the collapsed Properties section
  await expect(form.locator('select[name="titleFormat"]')).toBeVisible({ timeout: 10_000 });
  await set(form);
  await page.screenshot({ path: `screenshots/${shot}-configure-dialog.png` });
  await confirmWidgetConfigure(page);
  await page.getByRole('button', { name: 'Save' }).click();
  await expect(page.getByText(/Processing\. Please wait/)).toBeHidden({ timeout: 60_000 });
  await publishBlogIndex(page);
}

const entry = (page: Page, title: string) => page.locator('li.ui-perc-list-element', { hasText: title });

test.describe('Blog List Widget options', () => {
  test.setTimeout(300_000);
  const created = path.join(os.tmpdir(), `qa-bloglist-posts-${runKey}.txt`);

  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    if (!fs.existsSync(created)) {
      await ensureDeliveryResources(page);
      await ensureBlogInfrastructure(page);
      for (const n of POSTS) { const p = postFor(n); await createAndPublishPost(page, p.title, p.byline, p.body); }
      fs.writeFileSync(created, 'ok');
    }
  });

  test('Blog List: title element, texts, date format and the show/hide of date, byline, summary and comments from Configure are applied', async ({ page }) => {
    await configureBlogList(page, 'blog-list-texts', async f => {
      await f.locator('select[name="titleFormat"]').selectOption('h3', { timeout: 10_000 });
      await f.locator('input[name="blogPostDateTimeFormat"]').fill('yyyy-MM-dd');
      await f.locator('input[name="blogPostBylineText"]').fill('written by');
      await f.locator('input[name="blogPostCommentsText"]').fill('Remarks');
      await f.locator('input[name="blogPostReadMoreText"]').fill('Read on');
      for (const n of HIDE_FIELDS) await f.locator(`input[name="perc_hidefield_${n}"]`).uncheck();
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
      for (const n of HIDE_FIELDS) await f.locator(`input[name="perc_hidefield_${n}"]`).uncheck();
      await f.locator('select[name="sortby"]').selectOption('rx:page_title', { timeout: 10_000 });
      await f.locator('select[name="sortDirection"]').selectOption('asc', { timeout: 10_000 });
      await f.locator('input[name="blogPostMaxResults"]').fill('2');
    });
    await page.goto(`${DTS_URL}/${BLOG_FOLDER}/index.html`);
    const items = page.locator('ol.perc-blog-list.perc-list-main > li.ui-perc-list-element:visible');
    await expect(items, 'two entries on the first page').toHaveCount(2, { timeout: 60_000 });
    await page.screenshot({ path: 'screenshots/blog-list-published-index-two-per-page.png', fullPage: true });
  });

  test('Blog List: list id, aria label and role from Configure are written', async ({ page }) => {
    await configureBlogList(page, 'blog-list-aria-rss', async f => {
      await f.locator('input[name="blogPostMaxResults"]').fill('10');
      await f.locator('input[name="autoListDivId"]').fill(`qalist${runKey}`);
      await f.locator('input[name="listAriaLabel"]').fill('QA blog posts');
      await f.locator('input[name="listAriaRole"]').fill('region');
    });
    await page.goto(`${DTS_URL}/${BLOG_FOLDER}/index.html`);
    const box = page.locator(`#qalist${runKey}`);
    await expect(box, 'list id').toHaveCount(1, { timeout: 60_000 });
    await expect(box, 'aria label').toHaveAttribute('aria-label', 'QA blog posts');
    await expect(box, 'role').toHaveAttribute('role', 'region');
    await page.screenshot({ path: 'screenshots/blog-list-published-index-aria-and-rss.png', fullPage: true });
  });

  test('Blog List: calendar icon, French locale date and a total maximum of 2 from Configure are applied', async ({ page }) => {
    await configureBlogList(page, 'blog-list-calendar-locale-total', async f => {
      await setDefaults(f);
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
});
