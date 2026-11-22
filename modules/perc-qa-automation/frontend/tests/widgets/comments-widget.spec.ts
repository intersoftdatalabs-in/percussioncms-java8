import { test, expect } from '@playwright/test';
import { loginAsAdmin, createPage, publishOpenItem, configurePublishing, ensureDeliveryResources } from '../helpers/cms-ui';
import { placeWidget, saveLayout, openContentTab, widgetOnPage, editorFrame } from '../helpers/widgets-ui';
import { DTS_URL, DTS_ADMIN_URL, TEST_FOLDER } from '../helpers/qa-env';

/**
 * Comments Widget: lists the comments visitors submitted for the page. It is filled in the browser from the DTS comments service
 * (perc-comments-services), and the comments come from the Comments Form widget on the same page, so the test puts both widgets on a
 * page, publishes it, submits a comment on the PUBLISHED page served by the DTS and checks that the Comments widget shows it.
 */
test.describe('Comments Widget', () => {
  test.setTimeout(420_000);

  test('Comments Widget: a comment submitted with the Comments Form on the published page is listed by the Comments widget', async ({ page }) => {
    const run = Date.now().toString().slice(-7);
    const comment = { title: `QA comment title ${run}`, user: `QA Visitor ${run}`, text: `QA comment text ${run} submitted from the published page.`, email: `qa${run}@example.com` };

    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    await ensureDeliveryResources(page);

    const host = await createPage(page, `Comments Host ${run}`);
    await placeWidget(page, 'percCommentsForm');
    await placeWidget(page, 'percComments');
    await saveLayout(page);

    // the Comments Form comes with the usual fields (Title, User's name, Comment box, Email, Submit): store it as it is
    await openContentTab(page);
    await widgetOnPage(page, 'percCommentsForm').click();
    await editorFrame(page).locator('img[title="Edit local content"]').first().click();
    const dialog = page.locator('.ui-dialog:visible', { hasText: 'Edit Widget Content' });
    await expect(page.frameLocator('iframe#edit-widget-content-frame').getByText('Comment box')).toBeVisible({ timeout: 30_000 });
    await page.screenshot({ path: 'screenshots/comments-widget-comments-form-content-editor-default-fields.png' });
    await dialog.getByRole('button', { name: 'Save' }).click();
    await expect(dialog).toBeHidden({ timeout: 30_000 });
    await openContentTab(page);
    await publishOpenItem(page);

    await page.goto(`${DTS_URL}/${TEST_FOLDER}/${host}.html`);
    // A first-time visitor cannot submit (bug #338: the CSRF token is only returned once the cookie exists), so the page is viewed twice;
    // the first-visit failure is covered by the Comments Form Widget scenario.
    await page.locator('.perc-comments-form form').waitFor({ timeout: 60_000 });
    await page.reload();
    const form = page.locator('.perc-comments-form form');
    await expect(form, 'comment form on the published page').toBeVisible({ timeout: 60_000 });
    await form.getByLabel('Title').fill(comment.title);
    await form.getByLabel("User's name").fill(comment.user);
    await form.getByLabel('Comment box').fill(comment.text);
    await form.getByLabel('Email').fill(comment.email);
    await page.screenshot({ path: 'screenshots/comments-widget-published-page-comment-form-filled.png' });
    await form.getByRole('button', { name: 'Submit' }).click();

    await expect(page, 'after Submit the browser returns to the page').toHaveURL(/lastCommentId=\d+/, { timeout: 30_000 });
    const list = page.locator('.perc-comments-view');
    await expect(list.getByText(comment.text), 'submitted comment text listed by the Comments widget').toBeVisible({ timeout: 60_000 });
    await expect(list.getByText(comment.user).first(), 'commenter name listed').toBeVisible();
    await page.screenshot({ path: 'screenshots/comments-widget-published-page-lists-the-submitted-comment.png', fullPage: true });
  });
});
