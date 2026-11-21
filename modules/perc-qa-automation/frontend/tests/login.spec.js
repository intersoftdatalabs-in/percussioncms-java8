const { test, expect } = require('@playwright/test');
const { loginAsAdmin, BASE_URL } = require('./helpers/auth');

// Previously a standalone script (own chromium launch + process.exit), which the Playwright runner could not execute.
// Now a regular spec: same login check, but with assertions and the runner's browser/config.
test.describe('Login', () => {
  test('Admin can log in and lands on the CMS app', async ({ page }) => {
    await loginAsAdmin(page);

    const url = page.url();
    expect(url, `logged in against ${BASE_URL}`).toContain('/cm/app/');
    expect(url).not.toMatch(/error|login/i);
    await expect(page).not.toHaveTitle(/error/i);
  });
});
