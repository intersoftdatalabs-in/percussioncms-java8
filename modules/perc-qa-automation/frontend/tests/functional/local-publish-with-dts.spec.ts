import { test, expect } from '@playwright/test';
import * as fs from 'fs';
import { loginAsAdmin, selectPage, configurePublishing } from '../helpers/cms-ui';
import { PUBLISH_DIR, CMS_LOG, DTS_ADMIN_URL } from '../helpers/qa-env';

const OUT = PUBLISH_DIR;
const LOG = CMS_LOG;
const DTS_URL = DTS_ADMIN_URL;

test.describe('Local publish with DTS', () => {
  test.setTimeout(300_000);
  test('Local publish with DTS: page publishes to the DTS-backed site, metadata is delivered without errors and the page goes Live', async ({ page }) => {
    await loginAsAdmin(page);
    await expect(page.getByText('Welcome, Admin')).toBeVisible({ timeout: 90_000 });

    // select the DTS server in the site's publish server configuration (default output folder)
    await configurePublishing(page, DTS_URL, 'default');

    const logStart = fs.statSync(LOG).size;
    fs.rmSync(`${OUT}/index.html`, { force: true });

    await selectPage(page, 'QA-Widgets', 'index.html', true);
    await page.getByText('Publishing', { exact: true }).first().click({ timeout: 60_000 });
    await page.locator('a:visible, li:visible', { hasText: /^Publish$/ }).last().click();

    await expect.poll(() => fs.existsSync(`${OUT}/index.html`), { timeout: 90_000 }).toBeTruthy();
    await expect.poll(() => fs.readFileSync(LOG).subarray(logStart).toString().includes('Publishing Finished'), { timeout: 90_000 }).toBeTruthy();

    const newLog = fs.readFileSync(LOG).subarray(logStart).toString();
    const problems = newLog.split('\n').filter(l => /PSMetadataDeliveryHandler|PSDeliveryClient|Not workflowing/.test(l) && /ERROR|WARN/.test(l));
    expect(problems, 'no delivery errors while publishing to DTS').toEqual([]);

    await selectPage(page, 'QA-Widgets', 'index.html');
    await expect(page.getByText('Live (Default Workflow)')).toBeVisible({ timeout: 60_000 });
    await page.screenshot({ path: 'screenshots/local-publish-with-dts-page-live-after-publish.png' });
  });
});
