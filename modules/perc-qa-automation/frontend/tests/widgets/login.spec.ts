import { test, expect, Page } from '@playwright/test';
import { loginAsAdmin, createPage, publishOpenItem, configurePublishing, ensureDeliveryResources } from '../helpers/cms-ui';
import { placeWidget, saveLayout, openContentTab, openWidgetConfigure, confirmWidgetConfigure, openWidgetContentEditor } from '../helpers/widgets-ui';
import { DTS_URL, DTS_ADMIN_URL, TEST_FOLDER } from '../helpers/qa-env';

/**
 * Login: either a link to the site's login page or (Render mode "Show login form") a login form that signs a registered visitor in through the DTS
 * membership service. The proof is the PUBLISHED page served by the DTS: the link or form, the labels typed in the content editor, the validation
 * messages, the error for a wrong password, and a real sign-in (the visitor is registered through the membership service).
 */
test.describe.serial('Login', () => {
  test.setTimeout(420_000);
  const run = Date.now().toString().slice(-7);
  const email = `qalogin${run}@example.com`;
  const password = 'QaPass-12345';
  let formUrl = '';

  /** New page with a Login widget (Render mode, labels, layout and showUsername optional); publishes and returns the published URL. */
  async function publishLogin(page: Page, shot: string, mode: 'link' | 'form', labels?: { email: string; password: string; submit: string }, layout?: string, showUsername = false): Promise<string> {
    const host = await createPage(page, `Login Host ${shot.slice(-9)} ${run}`);
    await placeWidget(page, 'percLogin');
    const form = await openWidgetConfigure(page, 'percLogin');
    if (mode === 'form') await form.locator('select[name="perc-render-mode"]').selectOption('show_login_form', { timeout: 10_000 });
    if (layout) await form.locator('select[name="perc-label-layout"]').selectOption(layout, { timeout: 10_000 });
    await page.screenshot({ path: `screenshots/${shot}-configure-dialog.png` });
    await confirmWidgetConfigure(page);
    await saveLayout(page);
    const { frame, save } = await openWidgetContentEditor(page, 'percLogin');
    if (labels) {
      await frame.locator('#perc-content-edit-email_label').fill(labels.email);
      await frame.locator('#perc-content-edit-password_label').fill(labels.password);
      await frame.locator('#perc-content-edit-submit_label').fill(labels.submit);
    }
    if (showUsername) await frame.locator('input[name="show_username"]').check();
    await page.screenshot({ path: `screenshots/${shot}-content-editor.png` });
    await save.click();
    await expect(save).toBeHidden({ timeout: 60_000 });
    await openContentTab(page);
    await publishOpenItem(page);
    return `${DTS_URL}/${TEST_FOLDER}/${host}.html`;
  }

  test('Login: the default Render mode shows the "not logged in" message as a link to the login page', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    await ensureDeliveryResources(page);
    const url = await publishLogin(page, 'login-link-mode', 'link');
    await page.goto(url);
    const widget = page.locator('.percLogin');
    await expect(widget, 'Login widget on the published page').toBeVisible({ timeout: 60_000 });
    const link = widget.locator('a.perc-login-page-link');
    await expect(link, 'the message is a link').toBeVisible();
    await expect(link).not.toHaveText('');
    await expect(widget.locator('form.perc-login-form'), 'no form in link mode').toHaveCount(0);
    // the script adds the page the visitor came from, so the visitor returns to it after signing in
    await expect(link).toHaveAttribute('href', new RegExp(`\\?loginRedirect=${encodeURIComponent(url).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}$`));
    await page.screenshot({ path: 'screenshots/login-published-page-link-mode.png' });
  });

  test('Login: Render mode "Show login form" shows the form with the labels typed in the content editor', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    await ensureDeliveryResources(page);
    formUrl = await publishLogin(page, 'login-form-mode', 'form', { email: `Mail ${run}`, password: `Secret ${run}`, submit: `Enter ${run}` }, 'align_left', true);
    await page.goto(formUrl);
    const form = page.locator('form.perc-login-form');
    await expect(form, 'login form').toBeVisible({ timeout: 60_000 });
    await expect(form.getByText(`Mail ${run}`)).toBeVisible();
    await expect(form.getByText(`Secret ${run}`)).toBeVisible();
    await expect(form.locator('input[type=submit]'), 'submit button text').toHaveValue(`Enter ${run}`);
    await expect(form.locator('.field-label-container.align_left').first(), 'label layout "Align to the left" is applied').toBeVisible();
    await expect(page.locator('a.perc-login-page-link'), 'no link in form mode').toHaveCount(0);
    await page.screenshot({ path: 'screenshots/login-published-page-form-mode-custom-labels.png' });
  });

  test('Login: an empty form, a bad email and a short password show the validation messages and nothing is sent', async ({ page }) => {
    expect(formUrl).not.toBe('');
    const calls: string[] = [];
    page.on('request', r => { if (/membership\/login/.test(r.url())) calls.push(r.url()); });
    await page.goto(formUrl);
    const form = page.locator('form.perc-login-form');
    await expect(form).toBeVisible({ timeout: 60_000 });
    await form.locator('input[type=submit]').click();
    await expect(form.getByText('This field is required.'), 'the email is required').toBeVisible();
    await expect(form.getByText('Please provide a password.'), 'the password is required').toBeVisible();
    await form.locator('#perc-login-email-field').fill('not-an-email');
    await form.locator('#perc-login-password-field').fill('abc');
    await form.locator('input[type=submit]').click();
    await expect(form.getByText('Invalid Email address format.')).toBeVisible();
    await expect(form.getByText('Your password must be at least 6 characters long.')).toBeVisible();
    await page.screenshot({ path: 'screenshots/login-published-page-form-validation-messages.png' });
    expect(calls, 'nothing is sent to the membership service').toEqual([]);
  });

  test('Login: an unknown visitor is told the sign-in failed and stays signed out', async ({ page }) => {
    expect(formUrl).not.toBe('');
    await page.goto(formUrl);
    const form = page.locator('form.perc-login-form');
    await expect(form).toBeVisible({ timeout: 60_000 });
    await form.locator('#perc-login-email-field').fill(`nobody${run}@example.com`);
    await form.locator('#perc-login-password-field').fill('WrongPass-1');
    await form.locator('input[type=submit]').click();
    await expect(page.locator('.perc-login-error-message'), 'error message from the service').toContainText('Authentication failed', { timeout: 30_000 });
    expect((await page.context().cookies()).find(c => c.name === 'perc_membership_session_id'), 'no session cookie').toBeUndefined();
    await expect(form, 'form is still shown').toBeVisible();
    await page.screenshot({ path: 'screenshots/login-published-page-form-wrong-credentials.png' });
  });

  test('Login: a registered visitor signs in, sees the logged-in message with their email, and the form is gone', async ({ page }) => {
    expect(formUrl).not.toBe('');
    // set-up: the visitor is registered with the same call the Registration form makes (the Registration widget is not offered in the Add Widget library)
    const svc = `${DTS_URL}/perc-membership-services/membership`;
    await page.request.post(`${svc}/login`, { data: {} });   // the first call hands out the CSRF cookie
    const xsrf = (await page.request.storageState()).cookies.find(c => c.name === "XSRF-TOKEN")?.value ?? "";
    const reg = await page.request.post(`${svc}/user`, { headers: { "X-XSRF-TOKEN": xsrf }, data: { email, password, confirmationRequired: false, confirmationPage: `${DTS_URL}/` } });
    expect(await reg.json(), "visitor registered").toMatchObject({ status: "SUCCESS" });

    await page.goto(formUrl);
    const form = page.locator('form.perc-login-form');
    await expect(form).toBeVisible({ timeout: 60_000 });
    await form.locator('#perc-login-email-field').fill(email);
    await form.locator('#perc-login-password-field').fill(password);
    await form.locator('input[type=submit]').click();
    await expect.poll(async () => (await page.context().cookies()).some(c => c.name === 'perc_membership_session_id'), { timeout: 30_000, message: 'session cookie after sign-in' }).toBe(true);
    await expect(page.locator('.percLogin .login-contents-box'), 'logged-in message').toContainText(email, { timeout: 30_000 });
    await expect(page.locator('form.perc-login-form'), 'the form is removed once signed in').toHaveCount(0);
    await page.screenshot({ path: 'screenshots/login-published-page-signed-in.png' });
  });
});
