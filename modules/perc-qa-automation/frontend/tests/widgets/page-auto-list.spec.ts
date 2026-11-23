import { test, expect, Page } from '@playwright/test';
import { loginAsAdmin, createPage, publishOpenItem, configurePublishing, ensureDeliveryResources } from '../helpers/cms-ui';
import { placeWidget, saveLayout, openContentTab, openWidgetContentEditor, openWidgetConfigure, confirmWidgetConfigure } from '../helpers/widgets-ui';
import { DTS_URL, DTS_ADMIN_URL, TEST_FOLDER } from '../helpers/qa-env';

/**
 * Page Auto List: lists the PUBLISHED pages that match the criteria set in the widget's content editor (here: link text contains a token). The CMS
 * writes the query into the page and the list is filled in the browser from the DTS, so the proof is the published page served by the DTS: which
 * pages are listed, their order, and the options chosen in Configure (list type, heading formats, sort, number per page, link target, date, hiding).
 * Four pages are created and published first: three whose link text contains the run token and one that does not.
 */
const run = Date.now().toString().slice(-7);
const token = `QA PAL ${run}`;
const names = ['Alpha', 'Bravo', 'Charlie'];
const titleOf = (n: string) => `${token} ${n}`;

/** Puts a Page Auto List on a new page, optionally configures it, sets the criteria (link text contains `contains`) and publishes. Returns the published URL. */
async function publishListPage(page: Page, shot: string, configure?: (form: ReturnType<Page['locator']>) => Promise<void>, contains = token): Promise<string> {
  const host = await createPage(page, `Page List Host ${shot.slice(-7)} ${run}`);
  await placeWidget(page, 'percPageAutoList');
  if (configure) {
    const form = await openWidgetConfigure(page, 'percPageAutoList');
    await configure(form);
    await page.screenshot({ path: `screenshots/${shot}-configure-dialog.png` });
    await confirmWidgetConfigure(page);
  }
  await saveLayout(page);
  const { frame, save } = await openWidgetContentEditor(page, 'percPageAutoList');
  await frame.locator('#perc-content-edit-displaytitle').pressSequentially(`Pages ${run}`, { delay: 10 });
  await frame.locator('#display_title_contains').pressSequentially(contains, { delay: 10 });
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

  test('Page Auto List: published page lists the pages whose link text contains the text, newest first, as links to those pages, and not the others', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    await ensureDeliveryResources(page);
    // auto-lists only show published pages: each page is published right after it is created
    for (const n of names) {
      hrefs[n] = `/${TEST_FOLDER}/${await createPage(page, titleOf(n))}.html`;
      await openContentTab(page);
      await publishOpenItem(page);
    }
    await createPage(page, `Unrelated page ${run}`);
    await openContentTab(page);
    await publishOpenItem(page);
    const url = await publishListPage(page, 'page-auto-list-title-filter');

    await page.goto(url);
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
    await expect(items(page).first()).toBeVisible({ timeout: 60_000 });
    await expect(page.locator('h2.perc-list-title', { hasText: `Pages ${run}` }), 'list title is a Heading 2').toBeVisible();
    await expect(list.locator('ol.perc-list-main:visible'), 'ordered list').toHaveCount(1);
    await expect(items(page).locator('h3 > a').filter({ hasText: token }), 'each item is a Heading 3 with a link').toHaveCount(3);
    await expect(items(page).locator('a').filter({ hasText: token }), 'Link text ascending').toHaveText([titleOf('Alpha'), titleOf('Bravo'), titleOf('Charlie')]);
    await expect(items(page).locator('a').first(), 'link target').toHaveAttribute('target', '_blank');
    await expect(items(page).locator('.perc-page-auto-list-date').first(), 'post date shown').not.toHaveText('');
    // with a list title the list is labelled by that title (aria-labelledby); the aria-label setting is only used when there is no list title
    await expect(list, 'labelled by the list title').toHaveAttribute('aria-labelledby', await page.locator('h2.perc-list-title').getAttribute('id') as string);
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
    await expect(items(page), 'two pages on the first page of results').toHaveCount(2, { timeout: 60_000 });
    await expect(items(page).locator('a')).toHaveText([titleOf('Alpha'), titleOf('Bravo')]);
    const paging = page.locator('.perc-pagination-container').first();
    await expect(paging, 'paging controls').toBeVisible();
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
    await expect(page.locator('.perc-page-auto-list'), 'list is hidden').toBeHidden({ timeout: 30_000 });
    await expect(page.locator('li.perc-list-element:visible')).toHaveCount(0);
    await page.screenshot({ path: 'screenshots/page-auto-list-published-page-hidden-when-empty.png' });
  });
});
