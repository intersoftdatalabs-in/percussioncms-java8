import { test, expect, Page } from '@playwright/test';
import { loginAsAdmin, publishOpenItem, configurePublishing, ensureLevel2Section, addLevel3Section, openPageByPath } from '../helpers/cms-ui';
import { placeWidget, openWidgetConfigure, confirmWidgetConfigure, saveLayout, openContentTab } from '../helpers/widgets-ui';
import { DTS_URL, DTS_ADMIN_URL } from '../helpers/qa-env';

/**
 * Breadcrumb: rendered by the CMS at publish time from the page's position in the site NAVIGATION (sections), so the test
 * builds a real trail - Home > level 2 section > level 3 section - puts the widget on the level 3 section's landing page and
 * checks the PUBLISHED page served by the DTS: the links, their order, the separator and the labels set in Configure.
 */
const LEVEL2_TITLE = 'QA Crumb Two';
const LEVEL2_FOLDER = 'qa-crumb-two';

/** Creates a level 3 section, puts a Breadcrumb widget on its landing page, configures it, publishes it. Returns the published URL path. */
async function publishBreadcrumbLandingPage(page: Page, level3Title: string, level3Folder: string, configure: (form: ReturnType<Page['locator']>) => Promise<void>, shot: string) {
  await ensureLevel2Section(page, LEVEL2_TITLE, LEVEL2_FOLDER);
  await addLevel3Section(page, level3Title, level3Folder);
  // navigation is built from PUBLISHED sections: publish the parent section's landing page so it is part of the trail
  await openPageByPath(page, [LEVEL2_FOLDER], 'index.html');
  await openContentTab(page);
  await publishOpenItem(page);
  await openPageByPath(page, [LEVEL2_FOLDER, level3Folder], 'index.html');
  await placeWidget(page, 'percNavBreadcrumb');
  const form = await openWidgetConfigure(page, 'percNavBreadcrumb');
  await configure(form);
  await page.screenshot({ path: `screenshots/${shot}-configure-dialog.png` });
  await confirmWidgetConfigure(page);
  await saveLayout(page);
  await openContentTab(page);
  await publishOpenItem(page);
  return `/${LEVEL2_FOLDER}/${level3Folder}/index.html`;
}

test.describe('Breadcrumb', () => {
  test.setTimeout(300_000);

  test('Breadcrumb: published landing page shows Home > section > current section as links with the default separator', async ({ page }) => {
    const run = Date.now().toString().slice(-6);
    const l3Title = `QA C3 ${run}`;
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const urlPath = await publishBreadcrumbLandingPage(page, l3Title, `qa-crumb-three-${run}`, async form => {
      await expect(form.locator('input[name="showHome"]')).toBeChecked();
      await expect(form.locator('input[name="showCurrentPage"]')).toBeChecked();
      await expect(form.locator('input[name="separator"]')).toHaveValue(' > ');
    }, 'breadcrumb-default');

    await page.goto(`${DTS_URL}${urlPath}`);
    const crumb = page.locator('.perc-breadcrumb nav ul.perc-breadcrumb-main');
    await expect(crumb, 'breadcrumb list is on the published page').toBeVisible({ timeout: 30_000 });
    await page.screenshot({ path: 'screenshots/breadcrumb-default-published-page-trail.png' });
    const links = crumb.locator('li a');
    await expect(links).toHaveText(['Home', LEVEL2_TITLE, l3Title]);
    await expect(links.nth(0)).toHaveAttribute('href', '/index.html');
    await expect(links.nth(1)).toHaveAttribute('href', new RegExp(`^/${LEVEL2_FOLDER}/`));
    await expect(links.nth(2)).toHaveAttribute('href', new RegExp(`^/${LEVEL2_FOLDER}/qa-crumb-three-${run}/`));
    await expect(crumb.locator('li').first()).toHaveClass(/perc-list-first/);
    await expect(crumb.locator('li').last()).toHaveClass(/perc-list-last/);
    await expect(crumb.locator('li').nth(1)).toContainText('>');             // separator between the entries
  });

  test('Breadcrumb: Show home off, Show current page off, custom separator and labels change the published trail', async ({ page }) => {
    const run = Date.now().toString().slice(-6);
    const l3Title = `QA C3 ${run}`;
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const urlPath = await publishBreadcrumbLandingPage(page, l3Title, `qa-crumb-three-${run}`, async form => {
      await form.locator('input[name="showHome"]').uncheck();
      await form.locator('input[name="showCurrentPage"]').uncheck();
      await form.locator('input[name="separator"]').fill(' / ');
      await form.locator('input[name="ariaLabel"]').fill('QA trail');
      await form.locator('input[name="navLabel"]').fill('QA breadcrumb nav');
    }, 'breadcrumb-custom');

    await page.goto(`${DTS_URL}${urlPath}`);
    await expect(page.locator('.perc-breadcrumb')).toHaveAttribute('aria-label', 'QA trail', { timeout: 30_000 });
    await page.screenshot({ path: 'screenshots/breadcrumb-custom-published-page.png' });
    await expect(page.locator('.perc-breadcrumb nav')).toHaveAttribute('aria-label', 'QA breadcrumb nav');
    await expect(page.locator('.perc-breadcrumb ul.perc-breadcrumb-main a'), 'only the parent section: no Home, no current section').toHaveText([LEVEL2_TITLE]);
  });
});
