import { test, expect } from '@playwright/test';
import { loginAsAdmin } from '../helpers/cms-ui';

test.describe('Install CMS - New Install', () => {
  test('Install CMS - New Install: fresh install serves login page and Admin can log in to the dashboard', async ({ page }) => {
    await page.goto('/');
    await expect(page).toHaveURL(/login/);
    await page.screenshot({ path: 'screenshots/install-cms-new-install-login-page.png' });
    await loginAsAdmin(page);
    await expect(page).not.toHaveURL(/login/, { timeout: 60_000 });
    await expect(page.getByText('Welcome, Admin')).toBeVisible({ timeout: 60_000 });
    await expect(page.getByText('Sites', { exact: true }).first()).toBeVisible({ timeout: 60_000 });
    await expect(page.getByText('Assets', { exact: true }).first()).toBeVisible();
    await expect(page.getByText('Loading', { exact: false })).toHaveCount(0, { timeout: 60_000 });
    await page.screenshot({ path: 'screenshots/install-cms-new-install-dashboard-after-login.png' });
  });
});
