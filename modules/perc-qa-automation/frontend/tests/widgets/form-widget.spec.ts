import * as fs from 'fs';
import { startSmtpSink, mailText } from '../helpers/smtp-sink';
import { newSubmissionCount, exportFormData } from '../helpers/forms-ui';
import { test, expect, Page, FrameLocator } from '@playwright/test';
import { loginAsAdmin, createPage, publishOpenItem, configurePublishing, ensureDeliveryResources } from '../helpers/cms-ui';
import { placeWidget, saveLayout, openContentTab, openWidgetContentEditor } from '../helpers/widgets-ui';
import { DTS_URL, DTS_ADMIN_URL, TEST_FOLDER, TEST_SITE, isLocalSmtpConfigured } from '../helpers/qa-env';

/**
 * Form widget: a form built in the widget's content editor and rendered on the page. A visitor submits it on the PUBLISHED page served by
 * the DTS, which stores the data and sends the browser to the "success" page (or the "error" page) chosen in the form's meta-data.
 */

/** Creates and publishes a plain page; returns its file name (without .html). */
async function publishPlainPage(page: Page, title: string): Promise<string> {
  const name = await createPage(page, title);
  await openContentTab(page);
  await publishOpenItem(page);
  return name;
}

/** "On success / error redirect to": Browse > Select ... Page dialog > Sites > QA-Widgets > folder > page > Save. */
async function choosePage(page: Page, frame: FrameLocator, browseIndex: 0 | 1, pageName: string) {
  await frame.getByRole('button', { name: 'Browse' }).nth(browseIndex).click({ timeout: 15_000 });
  const dlg = page.locator('.ui-dialog:visible').filter({ hasText: /Select (Success|Error) Page/ });
  await expect(dlg).toBeVisible({ timeout: 15_000 });
  await dlg.getByText(TEST_SITE, { exact: true }).dblclick({ timeout: 15_000 });
  await dlg.getByText(TEST_FOLDER, { exact: true }).dblclick({ timeout: 15_000 });
  await dlg.getByText(`${pageName}.html`, { exact: true }).click({ timeout: 15_000 });
  await dlg.getByRole('button', { name: 'Save' }).click();
  await expect(dlg).toBeHidden({ timeout: 15_000 });
}

/** New page with a Form widget: title, success/error pages, optional edits of the entry field; publishes. Returns the published URL. */
let lastFormTitle = '';
async function publishFormPage(page: Page, run: string, thanks: string, error: string, shot: string, field?: { label: string; defaultText: string }, customize?: (frame: FrameLocator, page: Page) => Promise<void>): Promise<string> {
  const host = await createPage(page, `Form Host ${shot.slice(-8)} ${run}`);
  await placeWidget(page, 'percForm');
  await saveLayout(page);
  const { frame, save } = await openWidgetContentEditor(page, 'percForm');
  // a form name must be unique in the system, so every form gets its own title
  const formTitle = `QA Form ${shot.replace(/[^a-z]/g, '').slice(-9)} ${run}`;
  lastFormTitle = formTitle;
  await frame.locator('#perc-form-title').pressSequentially(formTitle, { delay: 10 });
  await expect(frame.locator('#perc-form-name'), 'form name is proposed from the title').toHaveValue(formTitle.replace(/ /g, '-'));
  if (field) {
    // the second block is the entry field; its pencil opens the label / default text boxes in place
    const entry = frame.locator('.perc-form-field-wrapper').nth(1);
    await entry.locator('.toggle-editor').click({ timeout: 15_000 });
    await entry.locator('input[name="fieldLabel"]').fill(field.label);
    await entry.locator('input[name="defaultValue"]').fill(field.defaultText);
  }
  if (customize) await customize(frame, page);
  // the customize step may already have opened the Meta-data section
  if (!(await frame.locator('#perc-formbuild-success-url').isVisible())) await frame.getByText('Meta-data').first().click();
  await choosePage(page, frame, 0, thanks);
  await choosePage(page, frame, 1, error);
  await expect(frame.locator('#perc-formbuild-success-url')).not.toHaveValue('');
  await page.screenshot({ path: `screenshots/${shot}-content-editor.png` });
  await save.click();
  await expect(save).toBeHidden({ timeout: 60_000 });
  await openContentTab(page);
  await publishOpenItem(page);
  return `${DTS_URL}/${TEST_FOLDER}/${host}.html`;
}

test.describe.serial('Form widget', () => {
  test.setTimeout(480_000);
  const run = Date.now().toString().slice(-7);
  let thanks = '';
  let error = '';

  test('Form widget: a visitor submits the published form and is sent to the success page chosen for the form', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    await ensureDeliveryResources(page);
    thanks = await publishPlainPage(page, `QA Thanks ${run}`);
    error = await publishPlainPage(page, `QA Form Error ${run}`);
    const url = await publishFormPage(page, run, thanks, error, 'form-widget-default-field');

    await page.goto(url);
    const form = page.locator('.perc-form form');
    await expect(form, 'form on the published page').toBeVisible({ timeout: 60_000 });
    await expect(page.locator('.perc-form').getByText('Entry field label')).toBeVisible();
    await page.screenshot({ path: 'screenshots/form-widget-published-page-shows-the-form.png' });
    await form.locator('input[type=text]').first().fill(`QA entry ${run}`);
    await form.locator('input[type=submit], button[type=submit]').first().click();
    await expect(page, 'after Submit the browser is sent to the success page').toHaveURL(new RegExp(`${thanks}\\.html`), { timeout: 30_000 });
    await page.screenshot({ path: 'screenshots/form-widget-submit-lands-on-the-success-page.png' });
  });

  test('Form widget: label and default text edited in the form designer appear on the published form', async ({ page }) => {
    expect(thanks, 'success page from the first scenario').not.toBe('');
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const field = { label: `Your name ${run}`, defaultText: `Default text ${run}` };
    const url = await publishFormPage(page, run, thanks, error, 'form-widget-edited-field', field);

    await page.goto(url);
    const form = page.locator('.perc-form form');
    await expect(form).toBeVisible({ timeout: 60_000 });
    await expect(form.getByText(field.label), 'edited label').toBeVisible();
    await expect(form.getByText('Entry field label'), 'default label is replaced').toHaveCount(0);
    await expect(form.locator('input[type=text]').first(), 'default text is shown as the placeholder of the box').toHaveAttribute('placeholder', field.defaultText);
    await page.screenshot({ path: 'screenshots/form-widget-published-form-shows-edited-label-and-default-text.png' });
    await form.locator('input[type=submit], button[type=submit]').first().click();
    await expect(page, 'submitting the edited form also reaches the success page').toHaveURL(new RegExp(`${thanks}\\.html`), { timeout: 30_000 });
  });

  test('Form widget: every field type is rendered, a required field is checked, and the submitted values are stored', async ({ page }) => {
    expect(thanks, 'success page from the first scenario').not.toBe('');
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const L = (n: string) => `${n} ${run}`;
    const url = await publishFormPage(page, run, thanks, error, 'form-widget-field-types', undefined, async frame => {
      // control menu: a click on a control name adds it at the end of the form and opens its editor
      const add = async (cls: string) => {
        await frame.locator('#perc-control-menu-button').click({ timeout: 8_000 }).catch(() => {});
        await frame.locator(`li.${cls}`).click({ timeout: 8_000 });
        await expect(frame.locator('.perc-form-field-wrapper').last()).toBeVisible();
        return frame.locator('.perc-form-field-wrapper').last();
      };
      // the entry field that every new form has: label, required, name and maximum length through "Configure Form Field"
      const entry = frame.locator('.perc-form-field-wrapper').nth(1);
      await entry.locator('.toggle-editor').click({ timeout: 15_000 });
      await entry.locator('input[name="fieldLabel"]').fill(L('Full name'));
      await entry.locator('.toggle-configure').click({ timeout: 15_000 });
      const dlg = frame.locator('.ui-dialog:visible').filter({ hasText: 'Configure Form Field' });
      await dlg.locator('input[type=checkbox]').first().check({ timeout: 10_000 });
      await dlg.locator('input[type=text]').nth(1).fill('fullname');
      await dlg.locator('input[type=text]').nth(4).fill('20');
      await dlg.getByText('Apply', { exact: true }).click();
      await expect(dlg, 'Apply closes the dialog').toBeHidden({ timeout: 10_000 });

      let w = await add('form-textarea-label');   await w.locator('input[name="fieldLabel"]').fill(L('Message'));
      w = await add('form-drop-down-label');      await w.locator('input[name="fieldLabel"]').fill(L('Colour'));
      w = await add('form-check-boxes-label');    await w.locator('input[name="fieldLabel"]').fill(L('Extras'));
      w = await add('form-radio-buttons-label');  await w.locator('input[name="fieldLabel"]').fill(L('Size'));
      w = await add('form-date-field-label');     await w.locator('input[name="fieldLabel"]').fill(L('Date'));
      w = await add('form-hidden-field-label');
      await w.locator('input[name="fieldName"]').fill('qahidden');
      await w.locator('input[name="defaultValue"]').fill(`hidden-${run}`);
      await add('form-perc-honeypot-label');
      await page.screenshot({ path: 'screenshots/form-widget-field-types-content-editor.png' });
    });

    await page.goto(url);
    const form = page.locator('.perc-form form');
    await expect(form).toBeVisible({ timeout: 60_000 });
    await page.reload();    // bug #338: the token for the submit is only there from the second page view on
    await expect(form).toBeVisible({ timeout: 60_000 });

    // what each control renders as
    await expect(form.getByLabel(L('Full name')), 'entry field with the maximum length from Configure').toHaveAttribute('fieldmaxlength', '20');
    await expect(form.locator('textarea'), 'text box').toHaveCount(1);
    await expect(form.locator('select option'), 'drop down with its three options').toHaveText(['Selection 1', 'Selection 2', 'Selection 3']);
    await expect(form.getByRole('checkbox'), 'three check boxes').toHaveCount(3);
    await expect(form.getByRole('radio'), 'three radio buttons').toHaveCount(3);
    await expect(form.getByRole('radio').first(), 'first radio button is preselected').toBeChecked();
    await expect(form.locator('input.hasDatepicker'), 'date field with a date picker').toHaveCount(1);
    await expect(form.locator('input[type=hidden][name="qahidden"]'), 'hidden field keeps its value').toHaveValue(`hidden-${run}`);
    await expect(form.locator('input#topyenoh'), 'honeypot field is not visible').toBeHidden();
    await page.screenshot({ path: 'screenshots/form-widget-published-form-with-every-field-type.png' });

    // the entry field was made required: submitting without it is refused in the browser
    await form.locator('input[type=submit]').first().click();
    await expect(form.getByText('This field is required.'), 'message under the empty required field').toHaveCount(1);
    await expect(page).not.toHaveURL(new RegExp(`${thanks}\\.html`));
    await page.screenshot({ path: 'screenshots/form-widget-empty-required-field-is-refused.png' });

    // fill everything in, with the date chosen from the date picker
    await form.getByLabel(L('Full name')).fill(`Tester ${run}`);
    await form.locator('textarea').fill(`Message text ${run}`);
    await form.locator('select').selectOption('Selection 2');
    await form.getByRole('checkbox').nth(0).check();
    await form.getByRole('checkbox').nth(2).check();
    await form.getByRole('radio').nth(1).check();
    await form.locator('img.ui-datepicker-trigger').click();
    await page.locator('#ui-datepicker-div td a.ui-state-default').first().click();
    const dateValue = await form.locator('input.hasDatepicker').inputValue();
    expect(dateValue, 'date chosen with the picker').not.toBe('');
    await form.locator('input[type=submit]').first().click();
    await expect(page, 'valid form: the browser is sent to the success page').toHaveURL(new RegExp(`${thanks}\\.html`), { timeout: 30_000 });

    // what the DTS stored, read back through the Forms Tracker
    const title = lastFormTitle;
    expect(await newSubmissionCount(page, title), 'one new submission is listed for the form').toBe(1);
    const csv = await exportFormData(page, title);
    for (const v of [`Tester ${run}`, `Message text ${run}`, 'Selection 2', 'Item 1', 'Item 3', dateValue, `hidden-${run}`]) expect(csv, `stored data contains "${v}"`).toContain(v);
    expect(csv, 'the radio choice is stored').toContain('Item 2');
    fs.writeFileSync('screenshots/form-widget-stored-submission.csv.txt', csv);
  });

  test('Form widget: a submission the DTS cannot process sends the browser to the error page chosen for the form', async ({ page }) => {
    expect(thanks, 'pages from the first scenario').not.toBe('');
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishFormPage(page, run, thanks, error, 'form-widget-error-redirect');

    await page.goto(url);
    await expect(page.locator('.perc-form form')).toBeVisible({ timeout: 60_000 });
    await page.reload();    // bug #338: the token for the submit is only there from the second page view on
    const form = page.locator('.perc-form form');
    await expect(form).toBeVisible({ timeout: 60_000 });
    // a submission without its form name cannot be processed by the DTS (a damaged or tampered post)
    await form.locator('input[type=text]').first().fill(`error case ${run}`);
    await form.locator('input[name="perc_formName"]').evaluate(e => { (e as HTMLInputElement).value = ''; });
    await form.locator('input[type=submit]').first().click();
    await expect(page, 'the browser is sent to the error page, not the success page').toHaveURL(new RegExp(`${error}\\.html`), { timeout: 30_000 });
    await expect(page).not.toHaveURL(new RegExp(`${thanks}\\.html`));
    await page.screenshot({ path: 'screenshots/form-widget-failed-submission-lands-on-the-error-page.png' });
    expect(await newSubmissionCount(page, lastFormTitle), 'nothing was stored for the failed submission').toBe(0);
  });

  /** Opens Meta-data, ticks `checkbox` (Notify on form submission), checks that Mail to and Mail subject are required (Save is refused while they are empty) and fills them. */
  const emailSettings = (checkbox: string, to: string, subject: string) => async (frame: FrameLocator, page: Page) => {
    await frame.getByText('Meta-data').first().click();
    await frame.locator(checkbox).check({ timeout: 10_000 });
    await expect(frame.locator('#perc-formbuild-mail-to'), 'Mail to box appears').toBeVisible({ timeout: 10_000 });
    const dialog = page.locator('.ui-dialog:visible', { hasText: 'Edit Widget Content' });
    await dialog.getByRole('button', { name: 'Save' }).click({ timeout: 10_000, force: true }).catch(() => {});
    await expect(dialog, 'Save is refused while Mail to is empty: the editor stays open').toBeVisible();
    await expect(frame.getByText('This is a required field').first(), 'required message for Mail to').toBeVisible({ timeout: 10_000 });
    await frame.locator('#perc-formbuild-mail-to').fill(to);
    await frame.locator('#perc-formbuild-mail-subject').fill(subject);
    await page.screenshot({ path: 'screenshots/form-widget-email-settings-in-meta-data.png' });
  };

  test('Form widget: "Notify on form submission" sends a notification email and the submission is still stored', async ({ page }) => {
    test.skip(
      process.env.QA_FORM_EMAIL !== 'true' && !isLocalSmtpConfigured(),
      'needs DTS Deployment/Server/conf/perc/perc-email.properties pointed at localhost:2525 (no TLS) or QA_FORM_EMAIL=true',
    );
    expect(thanks, 'pages from the first scenario').not.toBe('');
    const sink = await startSmtpSink();
    try {
      await loginAsAdmin(page);
      await configurePublishing(page, DTS_ADMIN_URL, 'dts');
      const to = `qa-notify-${run}@example.com`;
      const subject = `QA form notification ${run}`;
      const url = await publishFormPage(page, run, thanks, error, 'form-widget-notify', undefined, emailSettings('#perc-form-metadata-email-notification', to, subject));

      await page.goto(url);
      await expect(page.locator('.perc-form form')).toBeVisible({ timeout: 60_000 });
      await page.reload();
      const form = page.locator('.perc-form form');
      await expect(form).toBeVisible({ timeout: 60_000 });
      await form.locator('input[type=text]').first().fill(`notify entry ${run}`);
      await form.locator('input[type=submit]').first().click();
      await expect(page, 'the visitor reaches the success page').toHaveURL(new RegExp(`${thanks}\\.html`), { timeout: 30_000 });

      await expect.poll(() => sink.mails.length, { message: 'a notification reaches the mail server', timeout: 45_000 }).toBeGreaterThan(0);
      const mail = sink.mails[0];
      expect(mail.to.join(' '), 'sent to the notification address').toContain(to);
      expect(mailText(mail), 'subject chosen for the notification').toContain(subject);
      expect(await newSubmissionCount(page, lastFormTitle), 'the submission is stored as well').toBe(1);
      fs.writeFileSync('screenshots/form-widget-notification-message.txt', mail.data);
    } finally { await sink.close(); }
  });
});

/**
 * reCaptcha control. It needs the DTS form service to have reCAPTCHA switched on (recaptcha.on=true and a secret in
 * Deployment/Server/conf/perc/perc-form-processor.properties) and internet access to Google. With it on, forms WITHOUT the control are treated
 * as spam, so these tests only run when QA_RECAPTCHA=true and the other Form scenarios must run with it off. The tests use Google's documented
 * test keys, which always pass.
 */
const RECAPTCHA_SITE_KEY = '6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI';

test.describe.serial('Form widget reCaptcha', () => {
  test.skip(process.env.QA_RECAPTCHA !== 'true', 'needs reCAPTCHA switched on in the DTS (QA_RECAPTCHA=true)');
  test.setTimeout(480_000);
  const run = Date.now().toString().slice(-7);

  test('Form widget reCaptcha: the published form shows the Google challenge, refuses a submit until it is ticked, and stores the checked submission', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    await ensureDeliveryResources(page);
    const thanks = await publishPlainPage(page, `QA Captcha Thanks ${run}`);
    const error = await publishPlainPage(page, `QA Captcha Error ${run}`);
    const url = await publishFormPage(page, run, thanks, error, 'form-widget-recaptcha', undefined, async frame => {
      await frame.locator('#perc-control-menu-button').click({ timeout: 8_000 }).catch(() => {});
      await frame.locator('li.form-perc-recaptcha-label').click({ timeout: 8_000 });
      // the control menu hides when the mouse leaves it, and the mouse is still over it: move away so the wrench of the new field can be reached
      await page.mouse.move(200, 750);
      await expect(frame.locator('#perc-control-menu-wrapper')).toBeHidden({ timeout: 5_000 });
      const w = frame.locator('.perc-form-field-wrapper').last();
      await w.locator('.toggle-configure').click({ timeout: 15_000 });
      await frame.locator('#perc-field-recaptcha-sitekey-value').fill(RECAPTCHA_SITE_KEY);
      await frame.getByText('Apply', { exact: true }).click();
      await page.screenshot({ path: 'screenshots/form-widget-recaptcha-site-key-in-content-editor.png' });
    });

    await page.goto(url);
    const form = page.locator('.perc-form form');
    await expect(form).toBeVisible({ timeout: 60_000 });
    await page.reload();    // bug #338: the token for the submit is only there from the second page view on
    await expect(form).toBeVisible({ timeout: 60_000 });
    await expect(form.locator('.g-recaptcha, [data-sitekey]').first(), 'reCAPTCHA container with the site key').toHaveAttribute('data-sitekey', RECAPTCHA_SITE_KEY);
    const challenge = page.frameLocator('iframe[src*="recaptcha"]').first();
    await expect(challenge.getByRole('checkbox'), 'the Google challenge loads in the page').toBeVisible({ timeout: 30_000 });
    await page.screenshot({ path: 'screenshots/form-widget-recaptcha-challenge-on-the-published-form.png' });

    // not ticked: the form is not sent
    await form.locator('input[type=text]').first().fill(`captcha entry ${run}`);
    await form.locator('input[type=submit]').first().click();
    await page.waitForTimeout(3_000);
    await expect(page, 'without the challenge ticked the browser stays on the form').not.toHaveURL(new RegExp(`${thanks}\\.html`));
    expect(await newSubmissionCount(page, lastFormTitle), 'nothing stored yet').toBe(0);

    // ticked (test key: always accepted): the form is sent and the DTS verifies the answer with Google
    await page.goto(url);
    await expect(form).toBeVisible({ timeout: 60_000 });
    await form.locator('input[type=text]').first().fill(`captcha entry ${run}`);
    await page.frameLocator('iframe[src*="recaptcha"]').first().getByRole('checkbox').click({ timeout: 30_000 });
    await expect(form.locator('textarea[name="g-recaptcha-response"]'), 'Google returns an answer token').not.toHaveValue('', { timeout: 30_000 });
    await form.locator('input[type=submit]').first().click();
    await expect(page, 'ticked: the visitor reaches the success page').toHaveURL(new RegExp(`${thanks}\\.html`), { timeout: 30_000 });
    expect(await newSubmissionCount(page, lastFormTitle), 'the checked submission is stored').toBe(1);
    expect(await exportFormData(page, lastFormTitle)).toContain(`captcha entry ${run}`);
  });

  test('Form widget reCaptcha: a post without the Google answer is not stored by the DTS', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    await ensureDeliveryResources(page);
    const thanks = await publishPlainPage(page, `QA Captcha2 Thanks ${run}`);
    const error = await publishPlainPage(page, `QA Captcha2 Error ${run}`);
    const url = await publishFormPage(page, run, thanks, error, 'form-widget-recaptcha-bypass', undefined, async frame => {
      await frame.locator('#perc-control-menu-button').click({ timeout: 8_000 }).catch(() => {});
      await frame.locator('li.form-perc-recaptcha-label').click({ timeout: 8_000 });
      // the control menu hides when the mouse leaves it, and the mouse is still over it: move away so the wrench of the new field can be reached
      await page.mouse.move(200, 750);
      await expect(frame.locator('#perc-control-menu-wrapper')).toBeHidden({ timeout: 5_000 });
      const w = frame.locator('.perc-form-field-wrapper').last();
      await w.locator('.toggle-configure').click({ timeout: 15_000 });
      await frame.locator('#perc-field-recaptcha-sitekey-value').fill(RECAPTCHA_SITE_KEY);
      await frame.getByText('Apply', { exact: true }).click();
    });

    await page.goto(url);
    await expect(page.locator('.perc-form form')).toBeVisible({ timeout: 60_000 });
    await page.reload();
    const form = page.locator('.perc-form form');
    await expect(form).toBeVisible({ timeout: 60_000 });
    // skip the browser check and send the form without the Google answer (what a script posting to the DTS would do)
    await form.locator('input[type=text]').first().fill(`bypass entry ${run}`);
    await Promise.all([
      page.waitForResponse(r => r.url().includes('/perc-form-processor/forms/form/collect') && r.request().method() === 'POST', { timeout: 30_000 }),
      form.evaluate(f => { f.querySelectorAll('[name="g-recaptcha-response"]').forEach(e => e.remove()); HTMLFormElement.prototype.submit.call(f); }),   // the Submit button is named "submit", so form.submit itself is shadowed
    ]);
    await expect(page, 'rejected submission lands on the error page').toHaveURL(new RegExp(`${error}\\.html`), { timeout: 30_000 });
    await expect(page, 'rejected submission does not reach the success page').not.toHaveURL(new RegExp(`${thanks}\\.html`));
    expect(await newSubmissionCount(page, lastFormTitle), 'the DTS did not store a submission without the Google answer').toBe(0);
  });
});
