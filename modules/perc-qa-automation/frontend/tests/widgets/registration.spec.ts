import * as fs from 'fs';
import { test, expect, Page } from '@playwright/test';
import { loginAsAdmin, createPage, publishOpenItem, configurePublishing } from '../helpers/cms-ui';
import { placeWidget, saveLayout, openContentTab, openWidgetContentEditor, openWidgetConfigure, confirmWidgetConfigure } from '../helpers/widgets-ui';
import { startSmtpSink } from '../helpers/smtp-sink';
import { DTS_URL, DTS_ADMIN_URL, TEST_FOLDER, isLocalSmtpConfigured } from '../helpers/qa-env';

/**
 * Registration (Deprecated): a visitor registration form of the membership service in three modes: Registration (with or without e-mail
 * verification), Password request (sends a reset link) and Password reset (sets a new password from that link). It runs in the browser on the PUBLISHED
 * page served by the DTS, so the proof is the page: the labels typed in the editor, the label layout, the validation messages, what happens on submit,
 * the e-mail the DTS sends (read from a local mail server) and whether the visitor can sign in through the membership service afterwards.
 */
process.env.QA_REG_RUN ??= Date.now().toString().slice(-7);   // shared with the workers Playwright restarts after a failed scenario
const run = process.env.QA_REG_RUN;
const SVC = `${DTS_URL}/perc-membership-services/membership`;
const password = 'QaPass-123456';

/** Calls the membership service the way the Login form does: the first call hands out the CSRF cookie that the next call sends back. */
async function signIn(page: Page, email: string, pw: string): Promise<{ status?: string; message?: string }> {
  await page.request.post(`${SVC}/login`, { data: {} });
  const xsrf = (await page.request.storageState()).cookies.find(c => c.name === 'XSRF-TOKEN')?.value ?? '';
  const res = await page.request.post(`${SVC}/login`, { headers: { 'X-XSRF-TOKEN': xsrf }, data: { email, password: pw } });
  return res.json();
}

/** A visitor registered directly in the membership service (no e-mail verification). */
async function registerVisitor(page: Page, email: string, pw: string) {
  await page.request.post(`${SVC}/login`, { data: {} });
  const xsrf = (await page.request.storageState()).cookies.find(c => c.name === 'XSRF-TOKEN')?.value ?? '';
  const res = await page.request.post(`${SVC}/user`, { headers: { 'X-XSRF-TOKEN': xsrf }, data: { email, password: pw, confirmationRequired: false, confirmationPage: `${DTS_URL}/` } });
  expect(await res.json(), `${email} registered`).toMatchObject({ status: 'SUCCESS' });
}

interface Opts {
  mode?: 'registration' | 'password_request' | 'password_reset';
  layout?: 'align_above' | 'align_left';
  verification?: boolean;
  labels?: { email: string; password: string; confirm: string; submit: string };
}

/** A new page with a Registration widget set up as `opts` says; published. Returns the published URL. */
async function publishRegistrationPage(page: Page, shot: string, opts: Opts = {}): Promise<string> {
  const host = await createPage(page, `Registration Host ${shot.replace(/^registration-/, '')} ${run}`);
  await placeWidget(page, 'percRegistration');
  if (opts.mode || opts.layout || opts.verification === false) {
    const form = await openWidgetConfigure(page, 'percRegistration');
    if (opts.mode) await form.locator('select[name="perc-registration-mode"]').selectOption(opts.mode, { timeout: 10_000 });
    if (opts.layout) await form.locator('select[name="perc-label-layout"]').selectOption(opts.layout, { timeout: 10_000 });
    if (opts.verification === false) await form.locator('input[name="perc-email-verification-required"]').uncheck();
    await page.screenshot({ path: `screenshots/${shot}-configure-dialog.png` });
    await confirmWidgetConfigure(page);
  }
  await saveLayout(page);
  const { frame, save } = await openWidgetContentEditor(page, 'percRegistration');
  if (opts.labels) {
    await frame.locator('#perc-content-edit-email_label').fill(opts.labels.email);
    await frame.locator('#perc-content-edit-password_label').fill(opts.labels.password);
    await frame.locator('#perc-content-edit-confirm_password_label').fill(opts.labels.confirm);
    await frame.locator('#perc-content-edit-submit_label').fill(opts.labels.submit);
  }
  await page.screenshot({ path: `screenshots/${shot}-content-editor.png` });
  await save.click();
  await expect(save).toBeHidden({ timeout: 60_000 });
  await openContentTab(page);
  await publishOpenItem(page);
  return `${DTS_URL}/${TEST_FOLDER}/${host}.html`;
}

const field = (page: Page, name: 'email' | 'password' | 'password-confirm') => page.locator(`#perc-registration-${name}-field`);
const errors = (page: Page) => page.locator('label.form-error-msg:visible');
const fill = async (page: Page, email: string, pw = password, confirm = pw) => {
  await field(page, 'email').fill(email);
  await field(page, 'password').fill(pw);
  await field(page, 'password-confirm').fill(confirm);
};

test.describe('Registration (Deprecated)', () => {
  test.setTimeout(240_000);
  test.skip(process.env.QA_FORM_EMAIL !== 'true' && !isLocalSmtpConfigured(), 'needs the DTS e-mail settings pointed at localhost:2525 (no TLS) or QA_FORM_EMAIL=true');

  test('Registration: the labels typed in the editor and the label layout "align left" are used, and the validation messages are shown', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const labels = { email: `Mail ${run}`, password: `Secret ${run}`, confirm: `Again ${run}`, submit: `Join ${run}` };
    const url = await publishRegistrationPage(page, 'registration-labels-validation', { layout: 'align_left', labels });
    await page.goto(url);
    await expect(page.locator('#perc-registration-cform'), 'with verification on, the form is the "cform"').toBeVisible({ timeout: 60_000 });
    await expect(page.locator('label[for="perc-registration-email-field"]'), 'e-mail label').toHaveText(labels.email);
    await expect(page.locator('label[for="perc-registration-password-field"]'), 'password label').toHaveText(labels.password);
    await expect(page.locator('label[for="perc-registration-password-confirm-field"]'), 'confirm password label').toHaveText(labels.confirm);
    await expect(page.locator('#perc-registration-cform input[type="submit"]'), 'submit label').toHaveValue(labels.submit);
    await expect(page.locator('.field-label-container').first(), 'label layout').toHaveClass(/align_left/);
    await page.screenshot({ path: 'screenshots/registration-published-page-custom-labels-align-left.png' });

    await page.locator('#perc-registration-cform input[type="submit"]').click();
    await expect(errors(page), 'all three fields are required').toHaveCount(3);
    await fill(page, 'not-an-email', '12345', '54321');
    await page.locator('#perc-registration-cform input[type="submit"]').click();
    await expect(errors(page).filter({ hasText: 'Invalid Email address format.' }), 'e-mail format').toHaveCount(1);
    await expect(errors(page).filter({ hasText: 'Your password must be at least 6 characters long.' }), 'short password').toHaveCount(1);
    await expect(errors(page).filter({ hasText: 'Please provide matching password.' }), 'password confirmation').toHaveCount(1);
    await page.screenshot({ path: 'screenshots/registration-published-page-validation-messages.png' });
  });

  test('Registration: with e-mail verification the visitor gets a link by e-mail, cannot sign in until it is opened, and can sign in afterwards', async ({ page }) => {
    const sink = await startSmtpSink();
    try {
      await loginAsAdmin(page);
      await configurePublishing(page, DTS_ADMIN_URL, 'dts');
      const url = await publishRegistrationPage(page, 'registration-verification');
      const email = `qa-reg-verify-${run}@example.com`;
      await page.goto(url);
      await expect(page.locator('#perc-registration-cform')).toBeVisible({ timeout: 60_000 });
      await fill(page, email);
      await page.locator('#perc-registration-cform input[type="submit"]').click();
      await expect(page.locator('.perc-reg-confirmation-message'), 'thank-you message').toContainText('Thank you for registering', { timeout: 30_000 });
      await expect(page.locator('#perc-registration-cform'), 'the form is gone').toBeHidden();
      await page.screenshot({ path: 'screenshots/registration-published-page-check-your-email.png' });

      await expect.poll(() => sink.mails.length, { message: 'the verification e-mail reaches the mail server', timeout: 45_000 }).toBeGreaterThan(0);
      const mail = sink.mails.find(m => m.to.join(' ').includes(email))!;
      expect(mail, 'sent to the registered address').toBeTruthy();
      fs.writeFileSync('screenshots/registration-verification-mail.txt', mail.data);
      const link = mail.data.match(/https?:\/\/[^\s"'<>]*rvkey=[^\s"'<>&]+/)?.[0];
      expect(link, 'the e-mail has a confirmation link with a key').toBeTruthy();
      expect((await signIn(page, email, password)).status, 'cannot sign in before the address is confirmed').not.toBe('SUCCESS');

      await page.goto(link!);
      await expect.poll(async () => (await signIn(page, email, password)).status, { message: 'can sign in once the link was opened', timeout: 30_000 }).toBe('SUCCESS');
    } finally { await sink.close(); }
  });

  test('Registration: with e-mail verification off the visitor is signed in at once and no e-mail is sent', async ({ page }) => {
    const sink = await startSmtpSink();
    try {
      await loginAsAdmin(page);
      await configurePublishing(page, DTS_ADMIN_URL, 'dts');
      const url = await publishRegistrationPage(page, 'registration-no-verification', { verification: false });
      const email = `qa-reg-direct-${run}@example.com`;
      await page.goto(url);
      await expect(page.locator('#perc-registration-form'), 'without verification the form is the plain one').toBeVisible({ timeout: 60_000 });
      await fill(page, email);
      await page.locator('#perc-registration-form input[type="submit"]').click();
      await expect.poll(async () => (await page.context().cookies()).some(c => c.name === 'perc_membership_session_id'), { message: 'a session cookie is set', timeout: 30_000 }).toBe(true);
      expect((await signIn(page, email, password)).status, 'the visitor can sign in').toBe('SUCCESS');
      expect(sink.mails.filter(m => m.to.join(' ').includes(email)), 'no e-mail is sent').toEqual([]);
    } finally { await sink.close(); }
  });

  test('Registration: an address that is already registered is refused with a message and the form stays', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishRegistrationPage(page, 'registration-duplicate', { verification: false });
    const email = `qa-reg-dup-${run}@example.com`;
    await registerVisitor(page, email, password);
    await page.goto(url);
    await expect(page.locator('#perc-registration-form')).toBeVisible({ timeout: 60_000 });
    await fill(page, email);
    await page.locator('#perc-registration-form input[type="submit"]').click();
    await expect(page.locator('.perc-reg-error-message'), 'error message shown').toBeVisible({ timeout: 30_000 });
    await expect(page.locator('.perc-reg-error-message')).not.toHaveText('');
    await expect(page.locator('#perc-registration-form'), 'the form stays').toBeVisible();
    await page.screenshot({ path: 'screenshots/registration-published-page-address-already-registered.png' });
  });

  test('Registration: Password request mails a reset link and Password reset sets a new password from it', async ({ page }) => {
    const sink = await startSmtpSink();
    try {
      await loginAsAdmin(page);
      await configurePublishing(page, DTS_ADMIN_URL, 'dts');
      const requestUrl = await publishRegistrationPage(page, 'registration-password-request', { mode: 'password_request' });
      const resetUrl = await publishRegistrationPage(page, 'registration-password-reset', { mode: 'password_reset' });
      const email = `qa-reg-reset-${run}@example.com`;
      await registerVisitor(page, email, password);

      await page.goto(requestUrl);
      await expect(page.locator('#perc-password-request'), 'password request form').toBeVisible({ timeout: 60_000 });
      await expect(page.locator('#perc-registration-password-field'), 'only the e-mail is asked for').toHaveCount(0);
      await page.locator('#perc-registration-email-field').fill(email);
      await page.locator('#perc-password-request input[type="submit"]').click();
      await expect(page.locator('.perc-confirmation-message'), 'confirmation message').toBeVisible({ timeout: 30_000 });
      await expect(page.locator('#perc-password-request'), 'the form is gone').toHaveCount(0);
      await page.screenshot({ path: 'screenshots/registration-published-page-reset-link-sent.png' });

      await expect.poll(() => sink.mails.filter(m => m.to.join(' ').includes(email)).length, { message: 'the reset e-mail reaches the mail server', timeout: 45_000 }).toBeGreaterThan(0);
      const key = sink.mails.find(m => m.to.join(' ').includes(email))!.data.match(/resetkey=([A-Za-z0-9_-]+)/)?.[1];
      expect(key, 'the e-mail has a reset key').toBeTruthy();

      await page.goto(`${resetUrl}?resetkey=${key}`);
      const form = page.locator('#perc-password-reset');
      await expect(form, 'the reset form appears for a valid key').toBeVisible({ timeout: 60_000 });
      await expect(form.locator('#perc-registration-email-field'), 'the e-mail is filled in').toHaveValue(email);
      await form.locator('#perc-registration-password-field').fill('QaNew-654321');
      await form.locator('#perc-registration-password-confirm-field').fill('QaNew-654321');
      await page.screenshot({ path: 'screenshots/registration-published-page-reset-form.png' });
      await form.locator('input[type="submit"]').click();
      await expect.poll(async () => (await page.context().cookies()).some(c => c.name === 'perc_membership_session_id'), { message: 'signed in after the reset', timeout: 30_000 }).toBe(true);
      expect((await signIn(page, email, 'QaNew-654321')).status, 'the new password works').toBe('SUCCESS');
      expect((await signIn(page, email, password)).status, 'the old password no longer works').not.toBe('SUCCESS');
    } finally { await sink.close(); }
  });

  test('Registration: the reset form is not shown without a valid reset key', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const resetUrl = await publishRegistrationPage(page, 'registration-reset-no-key', { mode: 'password_reset' });
    await page.goto(resetUrl);
    await expect(page.locator('#perc-password-reset'), 'attached but hidden without a key').toBeHidden({ timeout: 30_000 });
    await page.goto(`${resetUrl}?resetkey=not-a-real-key-${run}`);
    await page.waitForLoadState('networkidle');
    await expect(page.locator('#perc-password-reset'), 'hidden for a key that does not exist').toBeHidden();
  });
});
