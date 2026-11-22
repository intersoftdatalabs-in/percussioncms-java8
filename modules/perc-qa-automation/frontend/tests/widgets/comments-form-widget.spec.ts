import { test, expect, Page } from '@playwright/test';
import { loginAsAdmin, createPage, publishOpenItem, configurePublishing, ensureDeliveryResources } from '../helpers/cms-ui';
import { placeWidget, saveLayout, openContentTab, widgetOnPage, editorFrame, openWidgetConfigure, confirmWidgetConfigure } from '../helpers/widgets-ui';
import { DTS_URL, DTS_ADMIN_URL, TEST_FOLDER } from '../helpers/qa-env';

/**
 * Comments Form Widget: the form visitors use to leave a comment on a page. It posts to the DTS comments service
 * (perc-comments-services/comment/addcomment), so everything is checked on the PUBLISHED page served by the DTS.
 */

/** Creates a page with the Comments Form and Comments widgets, stores the form with its default fields, publishes it. Returns the published URL. */
async function publishCommentsFormPage(page: Page, run: string, labelAlign: 'top' | 'left', shot: string): Promise<string> {
  const host = await createPage(page, `Comments Form Host ${run}`);
  await placeWidget(page, 'percCommentsForm');
  await placeWidget(page, 'percComments');
  const form = await openWidgetConfigure(page, 'percCommentsForm');
  await form.locator('select[name="labelalign"]').selectOption(labelAlign);
  await page.screenshot({ path: `screenshots/${shot}-configure-dialog.png` });
  await confirmWidgetConfigure(page);
  await saveLayout(page);

  await openContentTab(page);
  await widgetOnPage(page, 'percCommentsForm').click();
  await editorFrame(page).locator('img[title="Edit local content"]').first().click();
  const dialog = page.locator('.ui-dialog:visible', { hasText: 'Edit Widget Content' });
  await expect(page.frameLocator('iframe#edit-widget-content-frame').getByText('Comment box')).toBeVisible({ timeout: 30_000 });
  await page.screenshot({ path: `screenshots/${shot}-content-editor-default-fields.png` });
  await dialog.getByRole('button', { name: 'Save' }).click();
  await expect(dialog).toBeHidden({ timeout: 30_000 });
  await openContentTab(page);
  await publishOpenItem(page);
  return `${DTS_URL}/${TEST_FOLDER}/${host}.html`;
}

async function fillComment(form: ReturnType<Page['locator']>, c: { title: string; user: string; text: string; email: string }) {
  await form.getByLabel('Title').fill(c.title);
  await form.getByLabel("User's name").fill(c.user);
  await form.getByLabel('Comment box').fill(c.text);
  await form.getByLabel('Email').fill(c.email);
}

test.describe('Comments Form Widget', () => {
  test.setTimeout(420_000);

  test('Comments Form Widget: a first-time visitor submits a comment on the published page and it is stored and listed', async ({ page }) => {
    const run = Date.now().toString().slice(-7);
    const c = { title: `First visit title ${run}`, user: `First Visitor ${run}`, text: `First visit comment ${run}.`, email: `first${run}@example.com` };
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    await ensureDeliveryResources(page);
    const url = await publishCommentsFormPage(page, run, 'top', 'comments-form-widget-first-visit');

    // first view of the page in this browser: no comments cookie yet (bug #338)
    await page.goto(url);
    const form = page.locator('.perc-comments-form form');
    await expect(form).toBeVisible({ timeout: 60_000 });
    await fillComment(form, c);
    await form.getByRole('button', { name: 'Submit' }).click();
    await expect(page, 'after Submit the browser returns to the page').toHaveURL(/lastCommentId=\d+/, { timeout: 30_000 });
    await expect(page.locator('.perc-comments-view').getByText(c.text)).toBeVisible({ timeout: 30_000 });
    await page.screenshot({ path: 'screenshots/comments-form-widget-first-visit-comment-stored-and-listed.png' });
  });

  test('Comments Form Widget: published form shows the fields with required markers, validates them, honours the label alignment and stores a valid comment', async ({ page }) => {
    const run = Date.now().toString().slice(-7);
    const c = { title: `Form title ${run}`, user: `Form Visitor ${run}`, text: `Form comment ${run}.`, email: `form${run}@example.com` };
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    await ensureDeliveryResources(page);
    const url = await publishCommentsFormPage(page, run, 'left', 'comments-form-widget-fields-and-validation');

    await page.goto(url);
    await page.locator('.perc-comments-form form').waitFor({ timeout: 60_000 });
    await page.reload();    // see bug #338: the token is only available from the second page view on
    const root = page.locator('.perc-comments-form');
    const form = root.locator('form');
    await expect(form).toBeVisible({ timeout: 60_000 });
    await expect(root, 'label alignment chosen in Configure').toHaveClass(/perc-label-location-left/);
    for (const label of ['Title', 'URL', "User's name", 'Comment box', 'Email']) await expect(form.getByLabel(label), `${label} field`).toBeVisible();
    await expect(form.locator('.perc-form-error-asterisk'), "required markers on User's name, Comment box and Email").toHaveCount(3);
    await expect(form.locator('#topyenoh'), 'honeypot field is hidden').toBeHidden();
    await page.screenshot({ path: 'screenshots/comments-form-widget-published-form-fields-label-left.png' });

    // empty required fields are refused in the browser: no request is sent
    await form.getByRole('button', { name: 'Submit' }).click();
    await expect(form.getByText('This field is required.'), 'a message under each empty required field').toHaveCount(3, { timeout: 10_000 });
    await expect(page).not.toHaveURL(/lastCommentId/);
    await page.screenshot({ path: 'screenshots/comments-form-widget-empty-required-fields-show-errors.png' });

    // an invalid e-mail address is refused too
    await fillComment(form, { ...c, email: 'not-an-email' });
    await form.getByRole('button', { name: 'Submit' }).click();
    await expect(page).not.toHaveURL(/lastCommentId/);
    await expect(form.locator('#email')).toHaveValue('not-an-email');

    await form.getByLabel('Email').fill(c.email);
    await form.getByRole('button', { name: 'Submit' }).click();
    await expect(page, 'valid comment: the browser returns to the page').toHaveURL(/lastCommentId=\d+/, { timeout: 30_000 });
    await expect(page.locator('.perc-comments-view').getByText(c.text), 'stored comment listed').toBeVisible({ timeout: 30_000 });
    await page.screenshot({ path: 'screenshots/comments-form-widget-valid-comment-stored-and-listed.png' });
  });
});
