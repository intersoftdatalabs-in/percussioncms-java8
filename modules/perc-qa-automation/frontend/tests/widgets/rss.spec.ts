import * as fs from 'fs';
import * as path from 'path';
import { test, expect, Page } from '@playwright/test';
import { loginAsAdmin, createPage, publishOpenItem, configurePublishing, ensureDeliveryResources } from '../helpers/cms-ui';
import { placeWidget, saveLayout, openContentTab, openWidgetContentEditor, openWidgetConfigure, confirmWidgetConfigure } from '../helpers/widgets-ui';
import { DTS_URL, DTS_ADMIN_URL, DTS_WEB_ROOT, TEST_FOLDER } from '../helpers/qa-env';

/**
 * RSS: shows an external RSS feed. The DTS feeds service fetches the feed and the page script writes the feed title and the items (title, date,
 * description) with the elements chosen in Configure. The proof is the PUBLISHED page served by the DTS. To need no internet access the feed is a file
 * the spec writes into the DTS web root (and removes afterwards): 12 items, the first with HTML and a long description.
 */
process.env.QA_RSS_RUN ??= Date.now().toString().slice(-7);   // shared with the workers Playwright restarts after a failed scenario
const run = process.env.QA_RSS_RUN;
const FEED_FILE = `qa-feed-${run}.xml`;
const FEED_URL = `${DTS_URL}/${FEED_FILE}`;
const LONG = 'Long description that goes on and on '.repeat(6).trim();   // well over the default 100 characters

const feedItems = Array.from({ length: 12 }, (_, i) =>
  `<item><title>Item ${i + 1} ${run}</title><link>https://example.com/qa-${run}/${i + 1}</link>` +
  `<pubDate>Mon, ${String(2 + i).padStart(2, '0')} Mar 2026 10:30:00 GMT</pubDate>` +
  `<description>${i === 0 ? `&lt;b&gt;Bold&lt;/b&gt; ${LONG}` : `Description ${i + 1}`}</description></item>`).join('\n');
const feedXml = `<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0"><channel><title>QA Feed ${run}</title><link>https://example.com/qa-feed-${run}</link><description>QA feed</description>
${feedItems}
</channel></rss>`;

async function publishRssPage(page: Page, shot: string, configure?: (form: ReturnType<Page['locator']>) => Promise<void>): Promise<string> {
  const host = await createPage(page, `Rss Host ${shot.slice(-9)} ${run}`);
  await placeWidget(page, 'percRss');
  if (configure) {
    const form = await openWidgetConfigure(page, 'percRss');
    await configure(form);
    await page.screenshot({ path: `screenshots/${shot}-configure-dialog.png` });
    await confirmWidgetConfigure(page);
  }
  await saveLayout(page);
  const { frame, save } = await openWidgetContentEditor(page, 'percRss');
  await frame.locator('#perc-content-edit-rss_url').fill(FEED_URL);
  await save.click();
  await expect(save).toBeHidden({ timeout: 60_000 });
  await openContentTab(page);
  await publishOpenItem(page);
  return `${DTS_URL}/${TEST_FOLDER}/${host}.html`;
}

const feed = (page: Page) => page.locator('.perc-feed-widget');
const items = (page: Page) => feed(page).locator('.perc-feed-item');
async function open(page: Page, url: string) {
  await page.goto(url);
  await expect(feed(page), 'the feed has been read').toHaveAttribute('aria-busy', 'false', { timeout: 60_000 });
}

test.describe('RSS', () => {
  test.setTimeout(150_000);
  test.beforeAll(() => { fs.writeFileSync(path.join(DTS_WEB_ROOT, FEED_FILE), feedXml); });
  test.afterAll(() => { fs.rmSync(path.join(DTS_WEB_ROOT, FEED_FILE), { force: true }); });

  test('RSS: with the default options the feed title and one item (title, date, description cut to 100 characters without HTML) are shown', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    await ensureDeliveryResources(page);
    const url = await publishRssPage(page, 'rss-defaults');
    await open(page, url);
    await expect(feed(page).locator('.perc-feed-title'), 'feed title').toContainText(`QA Feed ${run}`);
    await expect(feed(page).locator('.perc-feed-title a'), 'feed title links to the feed site').toHaveAttribute('href', `https://example.com/qa-feed-${run}`);
    await expect(items(page), 'default item limit is 1').toHaveCount(1);
    await expect(items(page).locator('.perc-feed-item-title a'), 'item title links to the item').toHaveAttribute('href', `https://example.com/qa-${run}/1`);
    await expect(items(page).locator('.perc-feed-item-title a')).toHaveAttribute('target', '_blank');
    await expect(items(page).locator('.perc-feed-item-title'), 'item title text').toContainText(`Item 1 ${run}`);
    await expect(items(page).locator('.perc-feed-item-date'), 'item date').toContainText('2026');
    const d = await items(page).locator('.perc-feed-item-description').innerText();
    expect(d, 'HTML removed from the description').not.toContain('<b>');
    expect(d, 'description starts with the text').toMatch(/^Bold Long description/);
    expect(d.endsWith('...'), 'description is cut and ends with ...').toBe(true);
    expect(d.length, 'description is cut to 100 characters plus the dots').toBeLessThanOrEqual(103);
    await page.screenshot({ path: 'screenshots/rss-published-page-defaults.png' });
  });

  test('RSS: item limit, wrapping list, element formats, feed id and aria label from Configure are applied', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishRssPage(page, 'rss-limit-and-elements', async f => {
      await f.locator('select[name="perc-feed-item-limit"]').selectOption('limit_5', { timeout: 10_000 });
      await f.locator('select[name="wrappingElement"]').selectOption('ul', { timeout: 10_000 });
      await f.locator('input[name="wrappingElemAriaLabel"]').fill('QA feed items');
      await f.locator('select[name="titleElement"]').selectOption('h2', { timeout: 10_000 });
      await f.locator('select[name="itemTitleElement"]').selectOption('h3', { timeout: 10_000 });
      await f.locator('select[name="itemDateElement"]').selectOption('span', { timeout: 10_000 });
      await f.locator('select[name="itemDescriptionElement"]').selectOption('p', { timeout: 10_000 });
      await f.locator('input[name="feedId"]').fill(`qafeed${run}`);
      await f.locator('input[name="ariaRole"]').fill('complementary');
    });
    await open(page, url);
    await expect(items(page), 'five items').toHaveCount(5);
    await expect(feed(page).locator('ul[aria-label="QA feed items"] > li.perc-feed-item'), 'items are li in a ul with the aria label').toHaveCount(5);
    await expect(feed(page).locator('h2.perc-feed-title'), 'feed title is a Heading 2').toHaveCount(1);
    await expect(items(page).first().locator('h3.perc-feed-item-title'), 'item title is a Heading 3').toHaveCount(1);
    await expect(items(page).first().locator('span.perc-feed-item-date'), 'date is a span').toHaveCount(1);
    await expect(items(page).first().locator('p.perc-feed-item-description'), 'description is a p').toHaveCount(1);
    await expect(feed(page), 'feed id').toHaveAttribute('id', `qafeed${run}`);
    await expect(feed(page), 'aria role').toHaveAttribute('role', 'complementary');
    await page.screenshot({ path: 'screenshots/rss-published-page-five-items-in-list.png' });
  });

  test('RSS: turning off the feed title, date and description leaves only what is still switched on', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishRssPage(page, 'rss-hide-parts', async f => {
      await f.locator('input[name="perc-feed-show-title"]').uncheck();
      await f.locator('input[name="perc-feed-show-item-date"]').uncheck();
      await f.locator('input[name="perc-feed-show-item-description"]').uncheck();
    });
    await open(page, url);
    await expect(feed(page).locator('.perc-feed-title'), 'no feed title').toHaveCount(0);
    await expect(items(page).locator('.perc-feed-item-date'), 'no date').toHaveCount(0);
    await expect(items(page).locator('.perc-feed-item-description'), 'no description').toHaveCount(0);
    await expect(items(page).locator('.perc-feed-item-title'), 'item title still shown').toHaveCount(1);
    await page.screenshot({ path: 'screenshots/rss-published-page-only-item-title.png' });
  });

  test('RSS: item title off shows no item title, "Maximum characters in description" 20 cuts the description', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishRssPage(page, 'rss-description-length', async f => {
      await f.locator('input[name="perc-feed-show-item-title"]').uncheck();
      await f.locator('input[name="perc-feed-description-length"]').fill('20');
    });
    await open(page, url);
    await expect(items(page).locator('.perc-feed-item-title'), 'no item title').toHaveCount(0);
    const d = await items(page).locator('.perc-feed-item-description').innerText();
    expect(d.length, 'description cut to 20 characters plus the dots').toBeLessThanOrEqual(23);
    expect(d.endsWith('...')).toBe(true);
    await page.screenshot({ path: 'screenshots/rss-published-page-description-cut-to-20.png' });
  });

  test('RSS: the published date format from Configure is used', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishRssPage(page, 'rss-date-format', async f => {
      await f.locator('input[name="perc-feed-item-date-format"]').fill('yy-mm-dd');
    });
    await open(page, url);
    await expect(items(page).locator('.perc-feed-item-date'), 'date written as yy-mm-dd').toHaveText(/^\d{4}-\d{2}-\d{2}$/);
    await page.screenshot({ path: 'screenshots/rss-published-page-date-format.png' });
  });

  test('RSS: item limit "All" shows every item of the feed', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishRssPage(page, 'rss-all-items', async f => {
      await f.locator('select[name="perc-feed-item-limit"]').selectOption('limit_-1', { timeout: 10_000 });
    });
    await open(page, url);
    await expect(items(page), 'all 12 items of the feed').toHaveCount(12);
    await page.screenshot({ path: 'screenshots/rss-published-page-all-items.png' });
  });
});
