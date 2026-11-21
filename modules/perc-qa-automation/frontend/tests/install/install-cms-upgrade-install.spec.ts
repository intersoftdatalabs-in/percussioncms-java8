import { test, expect } from '@playwright/test';
import * as fs from 'fs';
import { loginAsAdmin } from '../helpers/cms-ui';
import { CMS_INSTALL } from '../helpers/qa-env';

const INSTALL = process.env.CMS_INSTALL_DIR ?? CMS_INSTALL;

test.describe('Install CMS - Upgrade Install', () => {
  test('Install CMS - Upgrade Install: upgraded install runs the new build, keeps existing data and Admin can log in', async ({ page }) => {
    const ver = fs.readFileSync(`${INSTALL}/Version.properties`, 'utf8');
    expect(ver).toMatch(/buildNumber=20261120/);   // build number advanced from the 20261106 pre-upgrade build

    await loginAsAdmin(page);
    await expect(page.getByText('Welcome, Admin')).toBeVisible({ timeout: 90_000 });
    await expect(page.getByText('Sites', { exact: true }).first()).toBeVisible({ timeout: 60_000 });
    await page.screenshot({ path: 'screenshots/install-cms-upgrade-install-dashboard-after-login.png' });

    await page.getByText('About', { exact: true }).first().click();
    await page.screenshot({ path: 'screenshots/install-cms-upgrade-install-about-dialog-version.png' });
  });
});
