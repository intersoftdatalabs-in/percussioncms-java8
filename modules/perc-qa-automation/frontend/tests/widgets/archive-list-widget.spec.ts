import { test, expect, Page, FrameLocator } from '@playwright/test';
import { loginAsAdmin, createPage, publishOpenItem, configurePublishing, ensureDeliveryResources } from '../helpers/cms-ui';
import { placeWidget, openWidgetConfigure, confirmWidgetConfigure, openWidgetContentEditor, saveLayout, openContentTab } from '../helpers/widgets-ui';
import { DTS_URL, DTS_ADMIN_URL, TEST_FOLDER, TEST_SITE, TEST_TEMPLATE } from '../helpers/qa-env';

/**
 * Archive List Widget: lists published pages grouped by the year/month they were created. The list is not in the
 * published HTML: the page carries a query, and the delivery-side script asks the DTS (perc-metadata-services
 * /metadata/blogs/get) for the matching pages. So the proof is on the PUBLISHED page served by the DTS.
 */

/** Number of published pages the DTS holds for a criteria list, per the same service the widget calls. */
async function dtsCount(page: Page, criteria: string[]): Promise<number> {
  const res = await page.request.post(`${DTS_URL}/perc-metadata-services/metadata/blogs/get`, { data: { criteria } });
  expect(res.ok(), 'DTS archive service answers').toBeTruthy();
  const body = await res.json();
  return (body.years ?? []).reduce((n: number, y: { yearCount: number }) => n + y.yearCount, 0);
}

/** Creates a page with an Archive List widget, configures it, fills the criteria in the content editor and publishes it. Returns the file name. */
async function publishArchiveHost(page: Page, run: string, fillCriteria: (frame: FrameLocator) => Promise<void>, shot: string) {
  const host = await createPage(page, `Archive Host ${run}`);
  await placeWidget(page, 'percArchiveList');
  const configure = await openWidgetConfigure(page, 'percArchiveList');
  await expect(configure.getByText('Layout:')).toBeVisible();
  await expect(configure.getByText('Number of entries to show:')).toBeVisible();
  await expect(configure.getByText('Locale:')).toBeVisible();
  await configure.locator('select').first().selectOption('perc-list-hierarchical');
  await page.screenshot({ path: `screenshots/${shot}-configure-dialog-layout-entries-locale.png` });
  await confirmWidgetConfigure(page);
  await saveLayout(page);

  const { frame, save } = await openWidgetContentEditor(page, 'percArchiveList');
  await frame.locator('input[name="displaytitle"]').first().fill(`Archives ${run}`);
  await fillCriteria(frame);
  await page.screenshot({ path: `screenshots/${shot}-content-editor-criteria.png` });
  await save.click();
  await expect(save).toBeHidden({ timeout: 30_000 });
  await openContentTab(page);
  await publishOpenItem(page);
  return host;
}

test.describe('Archive List Widget', () => {
  test.setTimeout(420_000);

  test('Archive List Widget: published page groups the pages of the chosen site and template by year and month with the same counts the DTS holds', async ({ page }) => {
    const run = Date.now().toString().slice(-7);
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    await ensureDeliveryResources(page);

    // three pages that are published to the DTS and must be counted
    for (const n of ['One', 'Two', 'Three']) {
      await createPage(page, `Archive Source ${run} ${n}`);
      await openContentTab(page);
      await publishOpenItem(page);
    }

    const host = await publishArchiveHost(page, run, async frame => {
      await frame.locator('.fancytree-title', { hasText: /^Sites$/ }).click();
      await frame.locator('.fancytree-title', { hasText: new RegExp(`^${TEST_SITE}$`) }).click();
      // the site can offer more than one template (e.g. leftovers of other tests): choose the one under test by name
      await frame.locator('div:has(> .perc-pagetemplates-chkbox)').filter({ hasText: TEST_TEMPLATE }).filter({ hasNotText: 'qa-blog-index' })
        .locator('.perc-pagetemplates-chkbox').check();
    }, 'archive-list-widget-site-and-template');

    const expected = await dtsCount(page, ["type = 'page'", `site = '${TEST_SITE}'`, `dcterms:source = '${TEST_TEMPLATE}'`]);
    expect(expected, 'at least the three pages published by this test are counted').toBeGreaterThanOrEqual(3);

    await page.goto(`${DTS_URL}/${TEST_FOLDER}/${host}.html`);
    await expect(page.locator('.perc-archive-title')).toHaveText(`Archives ${run}`, { timeout: 60_000 });
    const year = String(new Date().getFullYear());
    const monthName = new Date().toLocaleString('en-US', { month: 'long' });
    const yearItem = page.locator('li.perc-archive-year');
    await expect(yearItem, 'one year group on the published page').toHaveCount(1, { timeout: 60_000 });
    await expect(yearItem.locator('> a')).toHaveText(`${year} (${expected})`);
    await expect(yearItem.locator('li.perc-archive-month')).toHaveText(`${monthName} (${expected})`);
    await page.screenshot({ path: 'screenshots/archive-list-widget-published-page-shows-year-and-month-with-dts-count.png' });
  });

  test('Archive List Widget: Title contains filter limits the published list to the pages whose title contains the text', async ({ page }) => {
    const run = Date.now().toString().slice(-7);
    const marker = `QAArch${run}`;
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    await ensureDeliveryResources(page);

    for (const n of ['One', 'Two', 'Three']) {
      await createPage(page, `${marker} Source ${n}`);
      await openContentTab(page);
      await publishOpenItem(page);
    }
    await createPage(page, `Unrelated Archive Control ${run}`);   // must not be counted
    await openContentTab(page);
    await publishOpenItem(page);

    const host = await publishArchiveHost(page, run, async frame => {
      await frame.locator('#display_title_contains').fill(marker);
    }, 'archive-list-widget-title-contains');

    await page.goto(`${DTS_URL}/${TEST_FOLDER}/${host}.html`);
    await expect(page.locator('.perc-archive-title')).toHaveText(`Archives ${run}`, { timeout: 60_000 });
    const year = String(new Date().getFullYear());
    const monthName = new Date().toLocaleString('en-US', { month: 'long' });
    const yearItem = page.locator('li.perc-archive-year');
    await expect(yearItem, 'one year group with only the 3 matching pages').toHaveCount(1, { timeout: 60_000 });
    await expect(yearItem.locator('> a')).toHaveText(`${year} (3)`);
    await expect(yearItem.locator('li.perc-archive-month')).toHaveText(`${monthName} (3)`);
    await page.screenshot({ path: 'screenshots/archive-list-widget-title-contains-published-page-shows-3-pages.png' });
  });
});
