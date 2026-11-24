import { test, expect, Page, FrameLocator } from '@playwright/test';
import { loginAsAdmin, createPage, publishOpenItem, configurePublishing, ensureDeliveryResources, reopenPageInEditor } from '../helpers/cms-ui';
import { placeWidget, saveLayout, openContentTab, openWidgetContentEditor, openWidgetConfigure, confirmWidgetConfigure } from '../helpers/widgets-ui';
import { DTS_URL, DTS_ADMIN_URL, TEST_FOLDER } from '../helpers/qa-env';

/**
 * Page Auto List: lists the PUBLISHED pages that match the criteria set in the widget's content editor (here: link text contains a token). The CMS
 * writes the query into the page and the list is filled in the browser from the DTS, so the proof is the published page served by the DTS: which
 * pages are listed, their order, and the options chosen in Configure (list type, heading formats, sort, number per page, link target, date, hiding).
 * Four pages are created and published first: three whose link text contains the run token and one that does not.
 */
process.env.QA_PAL_RUN ??= Date.now().toString().slice(-7);   // shared with the workers Playwright restarts after a failed scenario
const run = process.env.QA_PAL_RUN;
const token = `QA PAL ${run}`;
const names = ['Alpha', 'Bravo', 'Charlie'];
const titleOf = (n: string) => `${token} ${n}`;

/** Puts a Page Auto List on a new page, optionally configures it, sets the criteria (link text contains `contains`) and publishes. Returns the published URL. */
async function publishListPage(page: Page, shot: string, configure?: (form: ReturnType<Page['locator']>) => Promise<void>, contains = token,
                                 extra?: (frame: FrameLocator) => Promise<void>, withTitle = true): Promise<string> {
  const host = await createPage(page, `Page List Host ${shot.replace(/^page-auto-list-/, '')} ${run} ${Date.now().toString().slice(-4)}`);
  await placeWidget(page, 'percPageAutoList');
  if (configure) {
    const form = await openWidgetConfigure(page, 'percPageAutoList');
    await configure(form);
    await page.screenshot({ path: `screenshots/${shot}-configure-dialog.png` });
    await confirmWidgetConfigure(page);
  }
  await saveLayout(page);
  const { frame, save } = await openWidgetContentEditor(page, 'percPageAutoList');
  if (withTitle) await frame.locator('#perc-content-edit-displaytitle').pressSequentially(`Pages ${run}`, { delay: 10 });
  await frame.locator('#display_title_contains').pressSequentially(contains, { delay: 10 });
  if (extra) await extra(frame);
  await page.screenshot({ path: `screenshots/${shot}-content-editor-criteria.png` });
  await save.click();
  await expect(save).toBeHidden({ timeout: 60_000 });
  await openContentTab(page);
  await publishOpenItem(page);
  return `${DTS_URL}/${TEST_FOLDER}/${host}.html`;
}

const items = (page: Page) => page.locator('li.perc-list-element:visible');

test.describe.serial('Page Auto List', () => {
  test.setTimeout(600_000);
  const hrefs: Record<string, string> = {};
  const names2: Record<string, string> = {};   // file names (without .html) of the pages
  const editUrls: Record<string, string> = {};

  test('Page Auto List: published page lists the pages whose link text contains the text, newest first, as links to those pages, and not the others', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    await ensureDeliveryResources(page);
    // auto-lists only show published pages: each page is published right after it is created
    for (const n of names) {
      names2[n] = await createPage(page, titleOf(n));
      hrefs[n] = `/${TEST_FOLDER}/${names2[n]}.html`;
      editUrls[n] = page.url();
      await openContentTab(page);
      await publishOpenItem(page);
      await page.waitForTimeout(1_500);   // the default sort is by post date (to the second): publish the pages at least a second apart
    }
    await createPage(page, `Unrelated page ${run}`);
    await openContentTab(page);
    await publishOpenItem(page);
    const url = await publishListPage(page, 'page-auto-list-title-filter');

    await page.goto(url);
    await expect(page.locator('.perc-page-auto-list'), 'list loaded from DTS').toHaveAttribute('aria-busy', 'false', { timeout: 60_000 });
    for (const n of names) await expect(page.locator('li.perc-list-element:visible', { hasText: titleOf(n) }), `"${titleOf(n)}" is listed`).toHaveCount(1, { timeout: 60_000 });
    await expect(page.getByText(`Unrelated page ${run}`), 'page whose link text does not match is not listed').toHaveCount(0);
    await expect(page.locator('.perc-list-title', { hasText: `Pages ${run}` }), 'list title').toBeVisible();
    // default sort is Post date, descending: the page published last (Charlie) is first
    await expect(items(page).locator('a').filter({ hasText: token })).toHaveText([titleOf('Charlie'), titleOf('Bravo'), titleOf('Alpha')]);
    for (const n of names) {
      const link = items(page).locator('a', { hasText: titleOf(n) });
      expect(new URL((await link.getAttribute('href'))!, page.url()).pathname, `link of ${n}`).toBe(hrefs[n]);
    }
    const res = await page.request.get(new URL(hrefs['Alpha'], url).toString());
    expect(res.status(), 'a listed page opens').toBe(200);
    await page.screenshot({ path: 'screenshots/page-auto-list-published-page-lists-matching-pages.png' });
  });

  test('Page Auto List: ordered list, list title and item heading formats, Link text ascending, link target, post date and aria role from Configure are applied', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishListPage(page, 'page-auto-list-formats-sort', async form => {
      await form.locator('select[name="listType"]').selectOption('ordered', { timeout: 10_000 });
      await form.locator('select[name="listTitleFormat"]').selectOption('h2', { timeout: 10_000 });
      await form.locator('select[name="itemTitleFormat"]').selectOption('h3', { timeout: 10_000 });
      await form.locator('select[name="sortby"]').selectOption('rx:resource_link_title', { timeout: 10_000 });
      await form.locator('select[name="sortDirection"]').selectOption('asc', { timeout: 10_000 });
      await form.locator('input[name="target"]').fill('_blank');
      await form.locator('input[name="showPostDate"]').check();
      await form.locator('input[name="listAriaRole"]').fill('navigation');
    });

    await page.goto(url);
    const list = page.locator('.perc-page-auto-list');
    await expect(list, 'list loaded from DTS').toHaveAttribute('aria-busy', 'false', { timeout: 60_000 });
    await expect(items(page).first()).toBeVisible({ timeout: 60_000 });
    await expect(page.locator('h2.perc-list-title', { hasText: `Pages ${run}` }), 'list title is a Heading 2').toBeVisible();
    await expect(list.locator('ol.perc-list-main:visible'), 'ordered list').toHaveCount(1);
    await expect(items(page).locator('h3 > a').filter({ hasText: token }), 'each item is a Heading 3 with a link').toHaveCount(3);
    await expect(items(page).locator('a').filter({ hasText: token }), 'Link text ascending').toHaveText([titleOf('Alpha'), titleOf('Bravo'), titleOf('Charlie')]);
    await expect(items(page).locator('a').first(), 'link target').toHaveAttribute('target', '_blank');
    await expect(items(page).locator('.perc-page-auto-list-date').first(), 'post date shown').not.toHaveText('');
    // with a list title the list is labelled by that title (aria-labelledby); the aria-label setting is only used when there is no list title
    const titleId = await page.locator('h2.perc-list-title').getAttribute('id');
    expect(titleId, 'the list title has an id').toBeTruthy();
    await expect(list, 'labelled by the list title').toHaveAttribute('aria-labelledby', titleId!);
    await expect(list, 'aria role').toHaveAttribute('role', 'navigation');
    await page.screenshot({ path: 'screenshots/page-auto-list-published-page-ordered-formats-link-text-ascending.png' });
  });

  test('Page Auto List: "Number of posts per page" 2 lists two pages and offers a second page of results', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishListPage(page, 'page-auto-list-two-per-page', async form => {
      await form.locator('input[name="maxlength"]').fill('2');
      await form.locator('select[name="sortby"]').selectOption('rx:resource_link_title', { timeout: 10_000 });
      await form.locator('select[name="sortDirection"]').selectOption('asc', { timeout: 10_000 });
    });
    await page.goto(url);
    await expect(page.locator('.perc-page-auto-list'), 'list loaded from DTS').toHaveAttribute('aria-busy', 'false', { timeout: 60_000 });
    await expect(items(page), 'two pages on the first page of results').toHaveCount(2, { timeout: 60_000 });
    await expect(items(page).locator('a')).toHaveText([titleOf('Alpha'), titleOf('Bravo')]);
    const paging = page.locator('.perc-pagination-container').first();
    await expect(paging, 'paging controls').toBeVisible({ timeout: 60_000 });
    await expect(paging).toContainText(/1 of 2 pages/);
    await paging.locator('a[aria-label="Page 2"]').click();
    await expect(items(page), 'one page on the second page of results').toHaveCount(1, { timeout: 30_000 });
    await expect(items(page).locator('a')).toHaveText([titleOf('Charlie')]);
    await page.screenshot({ path: 'screenshots/page-auto-list-published-page-second-page-of-results.png' });
  });

  test('Page Auto List: "Hide auto list when empty" hides the list when no page matches', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishListPage(page, 'page-auto-list-hide-when-empty', async form => {
      await form.locator('input[name="perc_hide_when_empty"]').check();
    }, `NoSuchPage ${run}`);
    await page.goto(url);
    await expect(page.locator('.perc-page-auto-list'), 'list is rendered').toBeAttached({ timeout: 30_000 });
    await expect(page.locator('.perc-page-auto-list'), 'list is hidden').toBeHidden({ timeout: 30_000 });
    await expect(page.locator('li.perc-list-element:visible')).toHaveCount(0);
    await page.screenshot({ path: 'screenshots/page-auto-list-published-page-hidden-when-empty.png' });
  });

  test('Page Auto List: div id, horizontal layout and the aria label from Configure are written when the list has no title', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishListPage(page, 'page-auto-list-div-id-horizontal-aria-label', async form => {
      await form.locator('input[name="autoListDivId"]').fill(`qapal${run}`);
      await form.locator('select[name="layout"]').selectOption('perc-list-horizontal', { timeout: 10_000 });
      await form.locator('input[name="listAriaLabel"]').fill('QA pages list');
    }, token, undefined, false);
    await page.goto(url);
    const list = page.locator(`#qapal${run}`);
    await expect(list, 'list div id').toHaveCount(1, { timeout: 30_000 });
    await expect(list, 'list loaded from DTS').toHaveAttribute('aria-busy', 'false', { timeout: 60_000 });
    await expect(list, 'horizontal layout class').toHaveClass(/perc-list-horizontal/);
    await expect(list, 'aria label').toHaveAttribute('aria-label', 'QA pages list');
    await expect(list, 'no labelledby without a title').not.toHaveAttribute('aria-labelledby', /.*/);
    await expect(list.locator('.perc-list-main-wrapper ul.perc-list-main'), 'an unordered list by default').toHaveCount(1);
    await expect(items(page), 'the three pages').toHaveCount(3);
    await page.screenshot({ path: 'screenshots/page-auto-list-published-page-div-id-horizontal-aria-label.png' });
  });

  test('Page Auto List: sorted by Link text descending and by Post date ascending the pages are in that order', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const titles = async () => (await items(page).locator('a').filter({ hasText: token }).allInnerTexts());
    const desc = await publishListPage(page, 'page-auto-list-sort-title-desc', async form => {
      await form.locator('select[name="sortby"]').selectOption('rx:resource_link_title', { timeout: 10_000 });
      await form.locator('select[name="sortDirection"]').selectOption('desc', { timeout: 10_000 });
    });
    await page.goto(desc);
    await expect(page.locator('.perc-page-auto-list'), 'list loaded from DTS').toHaveAttribute('aria-busy', 'false', { timeout: 60_000 });
    await expect(items(page)).toHaveCount(3, { timeout: 30_000 });
    expect(await titles(), 'Link text descending').toEqual([titleOf('Charlie'), titleOf('Bravo'), titleOf('Alpha')]);
    await page.screenshot({ path: 'screenshots/page-auto-list-published-page-sorted-by-link-text-descending.png' });
    const asc = await publishListPage(page, 'page-auto-list-sort-post-date-asc', async form => {
      await form.locator('select[name="sortby"]').selectOption('rx:sys_contentpostdate', { timeout: 10_000 });
      await form.locator('select[name="sortDirection"]').selectOption('asc', { timeout: 10_000 });
    });
    await page.goto(asc);
    await expect(page.locator('.perc-page-auto-list'), 'list loaded from DTS').toHaveAttribute('aria-busy', 'false', { timeout: 60_000 });
    await expect(items(page)).toHaveCount(3, { timeout: 30_000 });
    expect(await titles(), 'Post date ascending: the oldest page first').toEqual([titleOf('Alpha'), titleOf('Bravo'), titleOf('Charlie')]);
    await page.screenshot({ path: 'screenshots/page-auto-list-published-page-sorted-by-post-date-ascending.png' });
  });

  test('Page Auto List: the post date with its format and the French locale, and the calendar icon are written', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishListPage(page, 'page-auto-list-post-date-french-calendar-icon', async form => {
      await form.locator('input[name="showPostDate"]').check();
      await form.locator('input[name="postDateTimeFormat"]').fill('EEEE d MMMM yyyy');
      await form.locator('select[name="locale"]').selectOption('fr-FR', { timeout: 10_000 });
      await form.locator('input[name="enableCalenderIcon"]').check();
    });
    await page.goto(url);
    await expect(page.locator('.perc-page-auto-list'), 'list loaded from DTS').toHaveAttribute('aria-busy', 'false', { timeout: 60_000 });
    await expect(items(page)).toHaveCount(3, { timeout: 30_000 });
    await expect(items(page).locator('.perc-page-auto-list-date').first(), 'date in French with the chosen format').toHaveText(/^(lundi|mardi|mercredi|jeudi|vendredi|samedi|dimanche) \d{1,2} \p{L}+ \d{4}/u);
    await expect(items(page).locator('.perc-page-auto-list-calicon').first(), 'calendar icon').toBeVisible();
    await expect(items(page).locator('.perc-page-auto-list-calicon-day').first(), 'day on the calendar icon').toHaveText(/^\d{1,2}$/);
    await expect(items(page).locator('.perc-page-auto-list-calicon-month').first(), 'month on the calendar icon in French').toHaveText(/^[\p{L}.]{3,6}$/u);
    await page.screenshot({ path: 'screenshots/page-auto-list-published-page-post-date-french-calendar-icon.png' });
  });

  test('Page Auto List: the RSS icon is shown at the top or the bottom of the list with the link text from Configure', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const feed = async (frame: FrameLocator) => {
      await frame.getByText('Meta-data').first().click();
      await frame.locator('#perc-enable-feed').check({ timeout: 15_000 });
      await frame.locator('#perc-feed-title').fill(`QA Pages Feed ${run}`);
      await frame.locator('#perc-feed-name').fill(`qapalfeed${run}`);
      await frame.locator('#perc-feed-description').fill('Feed of the QA pages');
    };
    const top = await publishListPage(page, 'page-auto-list-rss-icon-top', async form => {
      await form.locator('input[name="enable_rss_icon"]').check();
      await form.locator('select[name="rssIconPosition"]').selectOption('perc-list-top', { timeout: 10_000 });
      await form.locator('input[name="rssLinkText"]').fill('QA feed top');
    }, token, feed);
    await page.goto(top);
    await expect(page.locator('.perc-rss-icon-top a.perc-rss-icon'), 'RSS icon at the top with its link text').toHaveAttribute('aria-label', 'QA feed top', { timeout: 60_000 });
    await expect(page.locator('.perc-rss-icon-top a.perc-rss-icon'), 'the link points to the feed').toHaveAttribute('href', new RegExp(`/feeds/rss/.+/qapalfeed${run}/`));
    await expect(page.locator('.perc-rss-icon-bottom'), 'no icon at the bottom').toHaveCount(0);
    await page.screenshot({ path: 'screenshots/page-auto-list-published-page-rss-icon-top.png' });
    const bottom = await publishListPage(page, 'page-auto-list-rss-icon-bottom', async form => {
      await form.locator('input[name="enable_rss_icon"]').check();
      await form.locator('select[name="rssIconPosition"]').selectOption('perc-list-bottom', { timeout: 10_000 });
      await form.locator('input[name="rssLinkText"]').fill('QA feed bottom');
    }, token, async frame => {
      await frame.getByText('Meta-data').first().click();
      await frame.locator('#perc-enable-feed').check({ timeout: 15_000 });
      await frame.locator('#perc-feed-title').fill(`QA Pages Feed B ${run}`);
      await frame.locator('#perc-feed-name').fill(`qapalfeedb${run}`);
      await frame.locator('#perc-feed-description').fill('Feed of the QA pages, bottom');
    });
    await page.goto(bottom);
    await expect(page.locator('.perc-rss-icon-bottom a.perc-rss-icon'), 'RSS icon at the bottom with its link text').toHaveAttribute('aria-label', 'QA feed bottom', { timeout: 60_000 });
    await expect(page.locator('.perc-rss-icon-top'), 'no icon at the top').toHaveCount(0);
    await page.screenshot({ path: 'screenshots/page-auto-list-published-page-rss-icon-bottom.png' });
  });

  test('Page Auto List: "Show page summary" lists the summary of a page, with the read more link', async ({ page }) => {
    expect(names2['Bravo'], 'pages from the first scenario').toBeTruthy();
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    // the summary of a page is generated by default (and empty here): give Bravo a summary of its own with the "More Link" marker
    await reopenPageInEditor(page, editUrls['Bravo'], names2['Bravo']);
    await expect(async () => {
      await page.getByText('Actions', { exact: true }).first().click();
      await page.getByText('Edit Metadata', { exact: true }).first().click({ timeout: 3_000 });
      await expect(page.locator('iframe#edit-page-metadata-frame')).toBeVisible({ timeout: 5_000 });
    }).toPass({ timeout: 60_000 });
    const meta = page.frameLocator('iframe#edit-page-metadata-frame');
    const label = meta.locator('.perc-section-label[group="perc-pageSum-container"]');
    await expect(async () => {
      if (!(await label.isVisible())) await meta.locator('#perc-content-edit-metadata-link').click({ timeout: 3_000 });
      await expect(label).toBeVisible({ timeout: 3_000 });
    }).toPass({ timeout: 60_000 });
    const auto = meta.locator('#perc-content-edit-auto_generate_summary');
    await expect(async () => {
      if (!(await auto.isVisible())) await label.click({ timeout: 3_000 });
      await expect(auto).toBeVisible({ timeout: 3_000 });
    }).toPass({ timeout: 30_000 });
    await auto.uncheck();
    await expect(meta.locator('.tox-tinymce:visible'), 'the summary editor appears when the summary is not generated').toBeVisible({ timeout: 15_000 });
    await meta.frameLocator('.tox-tinymce:visible .tox-edit-area__iframe').locator('body').click();
    await page.keyboard.type(`Own summary of the Bravo page ${run} `);
    await meta.locator('.tox-tinymce:visible').getByRole('button', { name: 'More Link' }).click();
    const dialog = page.locator('.ui-dialog:visible', { hasText: 'Edit Metadata' });
    await dialog.getByRole('button', { name: 'Save' }).click();
    await expect(dialog).toBeHidden({ timeout: 60_000 });
    await openContentTab(page);
    await publishOpenItem(page);

    const url = await publishListPage(page, 'page-auto-list-summary', async form => {
      await form.locator('input[name="summary"]').check();
    });
    await page.goto(url);
    await expect(page.locator('.perc-page-auto-list'), 'list loaded from DTS').toHaveAttribute('aria-busy', 'false', { timeout: 60_000 });
    const bravo = items(page).filter({ hasText: titleOf('Bravo') });
    await expect(bravo.locator('.perc-page-auto-list-summary-container'), 'the summary of the page').toContainText(`Own summary of the Bravo page ${run}`, { timeout: 30_000 });
    await expect(bravo.locator('a.perc-more-link, a.perc-blog-list-more-link'), 'read more link of the summary').toHaveCount(1);
    await expect(items(page).filter({ hasText: titleOf('Alpha') }).locator('.perc-page-auto-list-summary-container'), 'a page without a summary has no summary block').toHaveCount(0);
    await page.screenshot({ path: 'screenshots/page-auto-list-published-page-summary.png' });
  });

  test('Page Auto List: the paging text and the maximum number of total results from Configure are applied', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishListPage(page, 'page-auto-list-paging-text-total-max', async form => {
      await form.locator('input[name="maxlength"]').fill('1');
      await form.locator('input[name="totalMaxResults"]').fill('2');
      await form.locator('input[name="pagingPagesText"]').fill('QA pages');
      await form.locator('select[name="sortby"]').selectOption('rx:resource_link_title', { timeout: 10_000 });
      await form.locator('select[name="sortDirection"]').selectOption('asc', { timeout: 10_000 });
    });
    await page.goto(url);
    await expect(page.locator('.perc-page-auto-list'), 'list loaded from DTS').toHaveAttribute('aria-busy', 'false', { timeout: 60_000 });
    await expect(items(page), 'one page per page of results').toHaveCount(1, { timeout: 30_000 });
    const paging = page.locator('.perc-pagination-container').first();
    await expect(paging, 'only 2 of the 3 matching pages are counted, and the text for pages is the one from Configure').toContainText(/1 of 2 QA pages/, { timeout: 30_000 });
    await paging.locator('a[aria-label="Page 2"]').click();
    await expect(items(page).locator('a')).toHaveText([titleOf('Bravo')]);
    await expect(paging.locator('a[aria-label="Page 3"]'), 'no third page').toHaveCount(0);
    await page.screenshot({ path: 'screenshots/page-auto-list-published-page-paging-text-total-maximum.png' });
  });
});
