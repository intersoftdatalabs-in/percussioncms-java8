import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';
import { test, expect, Page, Locator } from '@playwright/test';
import { loginAsAdmin, configurePublishing, ensureDeliveryResources, openPageByPath, publishOpenItem } from '../helpers/cms-ui';
import { openWidgetConfigure, confirmWidgetConfigure, openContentTab } from '../helpers/widgets-ui';
import { ensureBlogInfrastructure, createAndPublishPost, BLOG_FOLDER, BLOG_POST_TEMPLATE } from '../helpers/blog-ui';
import { DTS_URL, DTS_ADMIN_URL, TEST_SITE } from '../helpers/qa-env';

/**
 * Blog Post Widget: sits on the blog POST template and shows one post. The post is created the supported way (New Blog wizard and
 * "new post" button of the dashboard Blogs gadget); its title, byline and body are typed in the widget's content editor, and the
 * proof is the PUBLISHED post page served by the DTS.
 */
test.describe('Blog Post Widget', () => {
  test.setTimeout(480_000);

  test('Blog Post Widget: published post page shows the title, byline and body typed in the widget content editor', async ({ page }) => {
    const run = Date.now().toString().slice(-7);
    const title = `QA Blog Post ${run}`;
    const byline = `QA Author ${run}`;
    const bodyText = `Body text of blog post ${run} written by the QA suite.`;

    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    await ensureDeliveryResources(page);
    await ensureBlogInfrastructure(page);
    await configureBlogPostTemplate(page, 'blog-post-defaults');   // the option scenarios change the template: start from the default options
    const name = await createAndPublishPost(page, title, byline, bodyText);

    await page.goto(`${DTS_URL}/${BLOG_FOLDER}/${name}.html`);
    await expect(page.getByText(title, { exact: false }).first(), 'post title on the published page').toBeVisible({ timeout: 30_000 });
    await expect(page.getByText(bodyText), 'post body on the published page').toBeVisible();
    await expect(page.getByText(byline), 'byline on the published page').toBeVisible();
    await page.screenshot({ path: 'screenshots/blog-post-widget-published-post-shows-title-byline-and-body.png' });
  });
});

/**
 * The options of the Blog Post widget are set on the widget in the blog POST TEMPLATE (Design > template > Layout > Configure). A post picks the change
 * up when it is published again, so every scenario sets its options, publishes the middle one of three posts again and checks the PUBLISHED page.
 */
process.env.QA_BLOGPOST_RUN ??= Date.now().toString().slice(-7);   // shared with the workers Playwright restarts after a failed scenario
const runKey = process.env.QA_BLOGPOST_RUN;
const names = path.join(os.tmpdir(), `qa-blogpost-posts-${runKey}.json`);
const POSTS = ['Older', 'Middle', 'Newer'];
const postFor = (n: string) => ({ title: `QA Opt Post ${n} ${runKey}`, byline: `QA Opt Author ${n} ${runKey}`, body: `Body of the ${n} option post ${runKey}.` });
const postFiles = (): Record<string, string> => JSON.parse(fs.readFileSync(names, 'utf8'));

async function setDefaults(f: Locator) {
  await f.locator('select[name="titleFormat"]').selectOption('h1', { timeout: 10_000 });
  await f.locator('select[name="locale"]').selectOption('en-US', { timeout: 10_000 });
  await f.locator('input[name="blogPostDateTimeFormat"]').fill("EEE MMM d, yyyy 'at' hh:mm a");
  await f.locator('input[name="blogPostBylineText"]').fill('by');
  await f.locator('input[name="perc_hidefield_blog_date"]').uncheck();
  await f.locator('input[name="perc_hidefield_byline"]').uncheck();
  await f.locator('select[name="blogNavigationLocation"]').selectOption('none', { timeout: 10_000 });
  await f.locator('select[name="blogNavigationContext"]').selectOption('preNext', { timeout: 10_000 });
  await f.locator('input[name="blogPostNextPost"]').fill('Newer Post');
  await f.locator('input[name="blogPostPrePost"]').fill('Older Post');
}

/** Resets the Blog Post widget on the post template to defaults, applies any overrides, and saves the template. */
async function configureBlogPostTemplate(page: Page, shot: string, set?: (form: Locator) => Promise<void>) {
  await page.goto('/cm/app/?view=design');
  await page.getByText(TEST_SITE, { exact: true }).first().dblclick({ timeout: 60_000 });
  await page.locator('.perc-template-item').filter({ hasText: BLOG_POST_TEMPLATE }).locator('button.perc-edit').click({ timeout: 60_000 });
  await page.getByText('Layout', { exact: true }).first().click();
  const form = await openWidgetConfigure(page, 'percBlogPost');
  await form.getByText('Properties', { exact: true }).click();   // the options are in the collapsed Properties section
  await expect(form.locator('select[name="titleFormat"]')).toBeVisible({ timeout: 10_000 });
  await setDefaults(form);
  if (set) await set(form);
  await page.screenshot({ path: `screenshots/${shot}-configure-dialog.png` });
  await confirmWidgetConfigure(page);
  await page.getByRole('button', { name: 'Save' }).click();
  await expect(page.getByText(/Processing\. Please wait/)).toBeHidden({ timeout: 60_000 });
}

/** Sets the options on the post template, then publishes the middle post again. Returns its published URL. */
async function configureBlogPost(page: Page, shot: string, set?: (form: Locator) => Promise<void>): Promise<string> {
  await configureBlogPostTemplate(page, shot, set);
  const middle = postFiles().Middle;
  await openPageByPath(page, [BLOG_FOLDER], `${middle}.html`);
  await openContentTab(page);
  await publishOpenItem(page);
  return `${DTS_URL}/${BLOG_FOLDER}/${middle}.html`;
}

test.describe('Blog Post Widget options', () => {
  test.setTimeout(300_000);

  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    if (!fs.existsSync(names)) {
      await ensureDeliveryResources(page);
      await ensureBlogInfrastructure(page);
      const made: Record<string, string> = {};
      for (const n of POSTS) { const p = postFor(n); made[n] = await createAndPublishPost(page, p.title, p.byline, p.body); }
      fs.writeFileSync(names, JSON.stringify(made));
    }
  });

  test('Blog Post: title element, byline text and date format from Configure are applied', async ({ page }) => {
    const url = await configureBlogPost(page, 'blog-post-texts', async f => {
      await f.locator('select[name="titleFormat"]').selectOption('h3', { timeout: 10_000 });
      await f.locator('input[name="blogPostBylineText"]').fill('written by');
      await f.locator('input[name="blogPostDateTimeFormat"]').fill('yyyy-MM-dd');
    });
    await page.goto(url);
    const post = page.locator('.perc-blog');
    await expect(post.locator('h3'), 'title is a Heading 3').toContainText(postFor('Middle').title, { timeout: 60_000 });
    await expect(post.locator('.perc-blog-dateByline-container'), 'byline text').toContainText(`written by ${postFor('Middle').byline}`);
    await expect(post.locator('.perc-blog-date'), 'date format').toHaveText(/\d{4}-\d{2}-\d{2}/);
    await page.screenshot({ path: 'screenshots/blog-post-published-texts-and-date-format.png' });
  });

  test('Blog Post: hiding the date and the byline removes them from the published post', async ({ page }) => {
    const url = await configureBlogPost(page, 'blog-post-hide', async f => {
      await f.locator('input[name="perc_hidefield_blog_date"]').check();
      await f.locator('input[name="perc_hidefield_byline"]').check();
    });
    await page.goto(url);
    await expect(page.locator('.perc-blog h1').first()).toContainText(postFor('Middle').title, { timeout: 60_000 });
    await expect(page.locator('.perc-blog-date'), 'date hidden').toHaveCount(0);
    await expect(page.locator('.perc-blog'), 'byline hidden').not.toContainText(postFor('Middle').byline);
    await page.screenshot({ path: 'screenshots/blog-post-published-date-and-byline-hidden.png' });
  });

  test('Blog Post: a French locale writes the date in French', async ({ page }) => {
    const url = await configureBlogPost(page, 'blog-post-french', async f => {
      await f.locator('select[name="locale"]').selectOption('fr-FR', { timeout: 10_000 });
      await f.locator('input[name="blogPostDateTimeFormat"]').fill('EEEE d MMMM yyyy');
    });
    await page.goto(url);
    await expect(page.locator('.perc-blog-date'), 'date in French').toHaveText(/(lundi|mardi|mercredi|jeudi|vendredi|samedi|dimanche) \d{1,2} \p{L}+ \d{4}/u, { timeout: 60_000 });
    await page.screenshot({ path: 'screenshots/blog-post-published-french-date.png' });
  });

  test('Blog Post: navigation above and beneath with the texts from Configure links to the older and the newer post', async ({ page }) => {
    const url = await configureBlogPost(page, 'blog-post-navigation', async f => {
      await f.locator('select[name="blogNavigationLocation"]').selectOption('aboveBeneath', { timeout: 10_000 });
      await f.locator('input[name="blogPostNextPost"]').fill('Go newer');
      await f.locator('input[name="blogPostPrePost"]').fill('Go older');
    });
    const file = postFiles();
    await page.goto(url);
    await expect(page.locator('.perc-blog-navigation-container'), 'navigation above and beneath').toHaveCount(2, { timeout: 60_000 });
    const top = page.locator('.perc-blog-nav-top');
    await expect(top.locator('.perc-blog-nav-left-wrapper a').first(), 'older post link text').toContainText('Go older');
    await expect(top.locator('.perc-blog-nav-right-wrapper a').first(), 'newer post link text').toContainText('Go newer');
    await expect(top.locator('.perc-blog-nav-left-wrapper a').first(), 'older link opens the older post').toHaveAttribute('href', new RegExp(`${file.Older}\\.html`), { timeout: 30_000 });
    await expect(page.locator('.perc-blog-nav-bottom .perc-blog-nav-right-wrapper a').first(), 'newer link opens the newer post').toHaveAttribute('href', new RegExp(`${file.Newer}\\.html`));
    await page.screenshot({ path: 'screenshots/blog-post-published-navigation-above-and-beneath.png', fullPage: true });
  });
});
