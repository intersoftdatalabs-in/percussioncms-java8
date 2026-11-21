import { test } from '@playwright/test';
import { loginAsAdmin, ensureTestSite } from '../helpers/cms-ui';

// Runs before the page, asset and widget specs (see the "setup" project in playwright.config.js).
// On a fresh install it creates the test site, its page template and its folder; otherwise it only checks they exist.
test('Test site: the site, template and folder used by the page and widget scenarios exist', async ({ page }) => {
  test.skip(process.env.QA_SKIP_SETUP === 'true', 'QA_SKIP_SETUP=true');
  test.setTimeout(180_000);
  await loginAsAdmin(page);
  await ensureTestSite(page);
});
