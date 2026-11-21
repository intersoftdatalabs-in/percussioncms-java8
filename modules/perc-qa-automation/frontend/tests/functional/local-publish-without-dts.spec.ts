import { test, expect } from '@playwright/test';
import * as fs from 'fs';
import { loginAsAdmin, selectPage, configurePublishing } from '../helpers/cms-ui';
import { PUBLISH_DIR } from '../helpers/qa-env';

const OUT = PUBLISH_DIR;

test.describe('Local publish without DTS', () => {
  test.setTimeout(300_000);
  test('Local publish without DTS: page publishes to the local file system and goes Live when no DTS is configured', async ({ page }) => {
    await loginAsAdmin(page);
    await expect(page.getByText('Welcome, Admin')).toBeVisible({ timeout: 90_000 });
    await configurePublishing(page, process.env.NO_DTS_LABEL ?? 'NONE', 'default');

    fs.rmSync(`${OUT}/index.html`, { force: true });
    await selectPage(page, 'QA-Widgets', 'index.html', true);
    await page.getByText('Publishing', { exact: true }).first().click({ timeout: 60_000 });
    await page.locator('a:visible, li:visible', { hasText: /^Publish$/ }).last().click();

    await expect.poll(() => fs.existsSync(`${OUT}/index.html`), { timeout: 90_000 }).toBeTruthy();
    const html = fs.readFileSync(`${OUT}/index.html`, 'utf8');
    expect(html).toContain('<html');
    await page.screenshot({ path: 'screenshots/local-publish-without-dts-page-live-after-publish.png' });
    // the page must reach the Live state (published), not stay Pending
    await expect(page.getByText(/Status:\s*Live|Live \(/).first()).toBeVisible({ timeout: 60_000 });
  });
});
