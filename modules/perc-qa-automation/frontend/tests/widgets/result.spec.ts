import { test, expect, Page } from '@playwright/test';
import { loginAsAdmin, createPage, publishOpenItem, configurePublishing, ensureDeliveryResources } from '../helpers/cms-ui';
import { placeWidget, saveLayout, openContentTab, openWidgetConfigure, confirmWidgetConfigure } from '../helpers/widgets-ui';
import { DTS_URL, DTS_ADMIN_URL, TEST_FOLDER } from '../helpers/qa-env';

/**
 * Result: shows the result of a search on the PUBLISHED page. The search itself (a search box, or a link) hands the Result widget a `filter`
 * (the words searched for) and a `query` (the criteria) in the page address; the widget asks the DTS metadata service for the matching pages and
 * lists them with their title and post date, with paging. The proof is the published page served by the DTS. Three pages are created and published
 * first (created in the order Bravo, Charlie, Alpha, so the title order, oldest first and newest first all differ).
 */
process.env.QA_RESULT_RUN ??= Date.now().toString().slice(-7);   // shared with the workers Playwright restarts after a failed scenario
const run = process.env.QA_RESULT_RUN;
const token = `QA RES ${run}`;
const created = ['Bravo', 'Charlie', 'Alpha'];   // creation order
const titleOf = (n: string) => `${token} ${n}`;

/** Address of the published page with the search handed over, as a search box would do: matching `contains` in the link text. */
const searchUrl = (base: string, contains: string) => {
  const query = { criteria: ["type = 'page'", `linktext LIKE '%${contains}%'`] };
  return `${base}?filter=${encodeURIComponent(contains)}&query=${encodeURIComponent(JSON.stringify(query))}`;
};

/** A new page with a Result widget (optionally configured), published. Returns the published URL without the search. */
async function publishResultPage(page: Page, shot: string, configure?: (form: ReturnType<Page['locator']>) => Promise<void>): Promise<string> {
  const host = await createPage(page, `Result Host ${shot.slice(-7)} ${run}`);
  await placeWidget(page, 'percResult');
  if (configure) {
    const form = await openWidgetConfigure(page, 'percResult');
    await configure(form);
    await page.screenshot({ path: `screenshots/${shot}-configure-dialog.png` });
    await confirmWidgetConfigure(page);
  }
  await saveLayout(page);
  await openContentTab(page);
  await publishOpenItem(page);
  return `${DTS_URL}/${TEST_FOLDER}/${host}.html`;
}

const view = (page: Page) => page.locator('.perc-result-view');
const titles = (page: Page) => view(page).locator('h3.perc-result-page-title');

async function search(page: Page, base: string, contains = token) {
  await page.goto(searchUrl(base, contains));
  await expect(view(page).locator('h2.perc-result-title'), 'the result is loaded').toBeVisible({ timeout: 60_000 });
}

test.describe('Result', () => {
  test.setTimeout(480_000);
  const hrefs: Record<string, string> = {};

  test('Result: default options list the matching pages newest first with the heading, the count and the post date, and a title opens its page', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    await ensureDeliveryResources(page);
    for (const n of created) {
      hrefs[n] = `/${TEST_FOLDER}/${await createPage(page, titleOf(n))}.html`;
      await openContentTab(page);
      await publishOpenItem(page);
      await page.waitForTimeout(1_500);   // the sort is by creation date: create the pages at least a second apart
    }
    await createPage(page, `Other page ${run}`);
    await openContentTab(page);
    await publishOpenItem(page);
    const base = await publishResultPage(page, 'result-defaults');

    await search(page, base);
    await expect(view(page).locator('h2.perc-result-title'), 'default header text then what was searched for').toHaveText(`Results for ${token}`);
    await expect(view(page).locator('p.perc-result-count'), 'count with the default plural text').toHaveText('3 entries');
    await expect(titles(page), 'newest first (Latest date is the default)').toHaveText([titleOf('Alpha'), titleOf('Charlie'), titleOf('Bravo')]);
    await expect(page.getByText(`Other page ${run}`), 'page that does not match is not listed').toHaveCount(0);
    await expect(view(page).locator('p.perc-result-page-date').first(), 'post date shown').toHaveText(/\d{4}/);
    await expect(view(page).locator('.perc-result-page-summary'), 'no summary by default').toHaveCount(0);
    await page.screenshot({ path: 'screenshots/result-published-page-default-results.png' });

    await titles(page).filter({ hasText: titleOf('Alpha') }).click();
    await expect(page, 'a result title opens that page').toHaveURL(new RegExp(hrefs['Alpha'].replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '$'), { timeout: 30_000 });
  });

  test('Result: sorting by Title and by Earliest date changes the order of the results', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const byTitle = await publishResultPage(page, 'result-sort-title', async f => {
      await f.locator('select[name="sortby"]').selectOption('rx:page_title', { timeout: 10_000 });
    });
    const earliest = await publishResultPage(page, 'result-sort-earliest', async f => {
      await f.locator('select[name="sortby"]').selectOption('rx:sys_contentpostdate', { timeout: 10_000 });
    });
    await search(page, byTitle);
    await expect(titles(page), 'Title: alphabetical').toHaveText([titleOf('Alpha'), titleOf('Bravo'), titleOf('Charlie')]);
    await page.screenshot({ path: 'screenshots/result-published-page-sorted-by-title.png' });
    await search(page, earliest);
    await expect(titles(page), 'Earliest date: oldest first').toHaveText([titleOf('Bravo'), titleOf('Charlie'), titleOf('Alpha')]);
  });

  test('Result: the header text, the entries wording (plural and singular), the paging words and "Number of items per page" from Configure are used', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const base = await publishResultPage(page, 'result-texts-and-paging', async f => {
      await f.locator('select[name="sortby"]').selectOption('rx:page_title', { timeout: 10_000 });
      await f.locator('input[name="maxResultsPerPage"]').fill('2');
      await f.locator('input[name="resultHeaderText"]').fill('Found for');
      await f.locator('input[name="resultEntriesTextPlural"]').fill('hits');
      await f.locator('input[name="resultEntriesTextSingular"]').fill('hit');
      await f.locator('input[name="pagingOfText"]').fill('out of');
      await f.locator('input[name="pagingPagesText"]').fill('sheets');
    });
    await search(page, base);
    await expect(view(page).locator('h2.perc-result-title'), 'header text').toHaveText(`Found for ${token}`);
    await expect(view(page).locator('p.perc-result-count'), 'plural wording').toHaveText('3 hits');
    await expect(titles(page), 'two results on the first page').toHaveText([titleOf('Alpha'), titleOf('Bravo')]);
    const paging = page.locator('.perc-pagination-container').first();
    await expect(paging, 'paging with the chosen words').toContainText('1 out of 2 sheets');
    await paging.locator('a[aria-label="Page 2"]').click();
    await expect(titles(page), 'second page of results').toHaveText([titleOf('Charlie')]);
    await page.screenshot({ path: 'screenshots/result-published-page-custom-texts-second-page.png' });

    await search(page, base, titleOf('Alpha'));
    await expect(view(page).locator('p.perc-result-count'), 'singular wording for one result').toHaveText('1 hit');
    await expect(titles(page)).toHaveText([titleOf('Alpha')]);
  });

  test('Result: the post date format from Configure is used', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const base = await publishResultPage(page, 'result-date-format', async f => {
      await f.locator('input[name="resultDateTimeFormat"]').fill('yyyy/MM/dd');
    });
    await search(page, base);
    await expect(view(page).locator('p.perc-result-page-date').first(), 'date written with the chosen format').toHaveText(/^\d{4}\/\d{2}\/\d{2}\s*$/);
    await page.screenshot({ path: 'screenshots/result-published-page-custom-date-format.png' });
  });

  test('Result: "Maximum number of total results" limits how many results are listed', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const base = await publishResultPage(page, 'result-total-max', async f => {
      await f.locator('input[name="totalMaxResults"]').fill('2');
    });
    await search(page, base);
    await expect(titles(page), 'only two of the three matching pages are listed').toHaveCount(2);
    await page.screenshot({ path: 'screenshots/result-published-page-total-maximum-two.png' });
  });

  test('Result: the date format locale from Configure writes the month name in that language', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const base = await publishResultPage(page, 'result-locale-french', async f => {
      await f.locator('select[name="locale"]').selectOption('fr-FR', { timeout: 10_000 });
      await f.locator('input[name="resultDateTimeFormat"]').fill('d MMMM yyyy');
    });
    await search(page, base);
    await expect(view(page).locator('p.perc-result-page-date').first(), 'French month name').toHaveText(/\d{1,2} (janvier|février|mars|avril|mai|juin|juillet|août|septembre|octobre|novembre|décembre) \d{4}/i);
    await page.screenshot({ path: 'screenshots/result-published-page-french-locale.png' });
  });
});
