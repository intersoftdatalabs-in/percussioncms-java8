import { test, expect, Page } from '@playwright/test';
import { loginAsAdmin, createPage, publishOpenItem, configurePublishing } from '../helpers/cms-ui';
import { placeWidget, saveLayout, openContentTab, openWidgetContentEditor, openWidgetConfigure, confirmWidgetConfigure } from '../helpers/widgets-ui';
import { DTS_URL, DTS_ADMIN_URL, TEST_FOLDER } from '../helpers/qa-env';

/**
 * Secure Login (Deprecated): a user name and password form that signs a visitor in to secured sections of the site. The sign-in itself is the
 * legacy "/perc-login" call of the DTS, which needs the secured-sections settings and an LDAP server (Deployment/Server/conf/perc/perc-secured-sections.properties);
 * the QA install has neither, so the DTS answers 404 there and the real sign-in cannot be tested. What can be proved is the PUBLISHED page: the form,
 * the title and labels typed in the editor, the label alignment and the "Forgot password?" link chosen in Configure.
 * The LDAP sign-in service of the DTS (perc-secure-membership, with sys_resources/webapps/secure) was removed from 8.1.8 (#297), so there is no sign-in to prove:
 * the last scenario records what a visitor gets when the form is submitted.
 */
process.env.QA_SL_RUN ??= Date.now().toString().slice(-7);   // shared with the workers Playwright restarts after a failed scenario
const run = process.env.QA_SL_RUN;

async function publishLoginPage(page: Page, shot: string, configure?: (f: ReturnType<Page['locator']>) => Promise<void>): Promise<string> {
  const host = await createPage(page, `Secure Login Host ${shot.replace(/^secure-login-/, '')} ${run}`);
  await placeWidget(page, 'percSecureLogin');
  if (configure) {
    const form = await openWidgetConfigure(page, 'percSecureLogin');
    await configure(form);
    await page.screenshot({ path: `screenshots/${shot}-configure-dialog.png` });
    await confirmWidgetConfigure(page);
  }
  await saveLayout(page);
  const { frame, save } = await openWidgetContentEditor(page, 'percSecureLogin');
  await frame.locator('#perc-content-edit-mainTitle').fill(`Members ${run}`);
  await frame.locator('#perc-content-edit-usernameLabel').fill(`User ${run}`);
  await frame.locator('#perc-content-edit-passwordLabel').fill(`Pass ${run}`);
  await frame.locator('#perc-content-edit-buttonLabel').fill(`Enter ${run}`);
  await page.screenshot({ path: `screenshots/${shot}-content-editor.png` });
  await save.click();
  await expect(save).toBeHidden({ timeout: 60_000 });
  await openContentTab(page);
  await publishOpenItem(page);
  return `${DTS_URL}/${TEST_FOLDER}/${host}.html`;
}

const form = (page: Page) => page.locator('form#login-form');

test.describe('Secure Login (Deprecated)', () => {
  test.setTimeout(150_000);

  test('Secure Login: the form shows the title and labels typed in the editor, labels above the fields and a Forgot password link by default', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishLoginPage(page, 'secure-login-defaults');
    await page.goto(url);
    await expect(form(page), 'the form is on the published page').toBeVisible({ timeout: 60_000 });
    await expect(form(page), 'the form posts to the secured sections login').toHaveAttribute('action', '/perc-login');
    await expect(form(page).locator('h2'), 'title').toHaveText(`Members ${run}`);
    await expect(form(page).locator('label').nth(0), 'user name label').toHaveText(`User ${run}`);
    await expect(form(page).locator('label').nth(1), 'password label').toHaveText(`Pass ${run}`);
    await expect(form(page).locator('input[type="submit"]'), 'button label').toHaveValue(`Enter ${run}`);
    await expect(form(page).locator('input[name="j_username"]'), 'user name field').toBeVisible();
    await expect(form(page).locator('input[name="j_password"]'), 'password field').toHaveAttribute('type', 'password');
    await expect(form(page).locator('.percLogin-form-label-top'), 'default label alignment is top').toHaveCount(2);
    await expect(form(page).getByRole('link', { name: 'Forgot password?' }), 'forgot password link is shown').toBeVisible();
    await expect(form(page).locator('.perc-secure-login-error-message'), 'the error message is hidden until a sign-in fails').toBeHidden();
    await page.screenshot({ path: 'screenshots/secure-login-published-page-defaults.png' });
  });

  test('Secure Login: label align "Left of the control" is applied and "Show forgot password link" off removes the link', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishLoginPage(page, 'secure-login-left-no-forgot', async f => {
      await f.locator('select[name="labelAlign"]').selectOption('left', { timeout: 10_000 });
      await f.locator('input[name="showForgotPasswordLink"]').uncheck();
    });
    await page.goto(url);
    await expect(form(page)).toBeVisible({ timeout: 60_000 });
    await expect(form(page).locator('.percLogin-form-label-left'), 'labels are to the left').toHaveCount(2);
    await expect(form(page).locator('.percLogin-form-label-top'), 'no label above the fields').toHaveCount(0);
    await expect(form(page).getByRole('link', { name: 'Forgot password?' }), 'no forgot password link').toHaveCount(0);
    await page.screenshot({ path: 'screenshots/secure-login-published-page-labels-left-no-forgot-link.png' });
  });

  test('Secure Login: submitting the form reaches /perc-login, which the DTS of 8.1.8 no longer serves (the LDAP sign-in service was removed in #297)', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishLoginPage(page, 'secure-login-submit');
    await page.goto(url);
    await expect(form(page)).toBeVisible({ timeout: 60_000 });
    await form(page).locator('input[name="j_username"]').fill('qa-user');
    await form(page).locator('input[name="j_password"]').fill('qa-password');
    const [response] = await Promise.all([
      page.waitForResponse(r => new URL(r.url()).pathname === '/perc-login' && r.request().method() === 'POST', { timeout: 30_000 }),
      form(page).locator('input[type="submit"]').click(),
    ]);
    expect(response.status(), 'no sign-in service answers /perc-login on the 8.1.8 DTS').toBe(404);
    await page.screenshot({ path: 'screenshots/secure-login-published-page-submit-no-sign-in-service.png' });
  });
});
