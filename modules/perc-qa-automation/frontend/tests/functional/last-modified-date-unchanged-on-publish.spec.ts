import { test, expect } from '@playwright/test';
import * as nodePath from 'path';
import { loginAsAdmin, selectPage, publishOpenItem, readItemDates, uploadFileAsset } from '../helpers/cms-ui';
import { FIXTURES_DIR, TEST_ASSET_FOLDER } from '../helpers/qa-env';

test.describe('Last modified date unchanged on publish', () => {
  test.setTimeout(300_000);

  test('Last modified date unchanged on publish - page: Last Modified of a page is unchanged after publish (Last Published advances)', async ({ page }) => {
    await loginAsAdmin(page);
    await selectPage(page, 'QA-Widgets', 'index.html');
    // The info panel first shows the parent site's details; wait for the page's own panel before reading dates.
    await expect(page.getByText('Page Link:')).toBeVisible();
    await expect.poll(async () => (await readItemDates(page)).modified).toBeTruthy();
    const before = await readItemDates(page);

    await page.getByText('index.html', { exact: true }).first().dblclick();
    await publishOpenItem(page);

    await selectPage(page, 'QA-Widgets', 'index.html');
    await expect(page.getByText('Page Link:')).toBeVisible();
    await expect.poll(async () => (await readItemDates(page)).published).toBeTruthy();
    const after = await readItemDates(page);
    expect(after.modified, 'Last Modified unchanged').toBe(before.modified);
    expect(after.published, 'Last Published present').toBeTruthy();
    await page.screenshot({ path: 'screenshots/last-modified-date-unchanged-on-publish-page-info-panel.png' });
  });

  test('Last modified date unchanged on publish - asset: Last Modified of an asset is unchanged after publish', async ({ page }) => {
    await loginAsAdmin(page);
    // A brand-new asset, so the test always covers the first publish of a never-published (Draft) asset
    const assetName = await uploadFileAsset(page, TEST_ASSET_FOLDER, nodePath.join(FIXTURES_DIR, 'word.docx'));
    const path = page.locator('#mcol-path-summary');
    const open = async (dbl: boolean) => {
      await page.goto('/cm/app/?view=dash');
      await page.getByText('Assets', { exact: true }).first().click();
      await expect(path).toHaveValue('/Assets');
      await page.getByText(TEST_ASSET_FOLDER, { exact: true }).first().click();
      await expect(path).toHaveValue(`/Assets/${TEST_ASSET_FOLDER}`);
      const it = page.getByText(assetName, { exact: true }).first();
      if (dbl) await it.dblclick(); else await it.click();
      await expect(path).toHaveValue(`/Assets/${TEST_ASSET_FOLDER}/${assetName}`);
    };
    await open(false);
    await expect.poll(async () => (await readItemDates(page)).modified).toBeTruthy();
    const before = await readItemDates(page);

    await open(true);
    await publishOpenItem(page);

    await open(false);
    await expect.poll(async () => (await readItemDates(page)).published).toBeTruthy();
    const after = await readItemDates(page);
    expect(after.modified, 'Last Modified unchanged').toBe(before.modified);
    await page.screenshot({ path: 'screenshots/last-modified-date-unchanged-on-publish-asset-info-panel.png' });
  });
});
