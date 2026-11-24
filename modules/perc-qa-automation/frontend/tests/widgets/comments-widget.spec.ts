import { test, expect } from '@playwright/test';
import { loginAsAdmin, createPage, publishOpenItem, configurePublishing, ensureDeliveryResources, reopenPageInEditor } from '../helpers/cms-ui';
import { placeWidget, saveLayout, openContentTab, widgetOnPage, editorFrame, openWidgetConfigure, confirmWidgetConfigure } from '../helpers/widgets-ui';
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
    // a first-time visitor submits straight away (bugs #338 and #373 are fixed): no second visit of the page is needed
    const form = page.locator('.perc-comments-form form');
    await expect(form, 'comment form on the published page').toBeVisible({ timeout: 60_000 });
    await form.getByLabel('Title').fill(comment.title);
    await form.getByLabel("User's name").fill(comment.user);
    await form.getByLabel('Comment box').fill(comment.text);
    await form.getByLabel('Email').fill(comment.email);
    await page.screenshot({ path: 'screenshots/comments-widget-published-page-comment-form-filled.png' });
    await form.getByRole('button', { name: 'Submit' }).click();
    await expect(page, 'the comment POST redirects back with the new comment id').toHaveURL(/lastCommentId=\d+/, { timeout: 30_000 });
    const list = page.locator('.perc-comments-view');
    await expect(list.getByText(comment.text), 'submitted comment text listed by the Comments widget').toBeVisible({ timeout: 60_000 });
    await expect(list.getByText(comment.user).first(), 'commenter name listed').toBeVisible();
    await page.screenshot({ path: 'screenshots/comments-widget-published-page-lists-the-submitted-comment.png', fullPage: true });
  });

  test('Comments Widget: "Heading format for Title field" writes the title of each comment as the chosen element', async ({ page }) => {
    const run = Date.now().toString().slice(-7);
    const title = `QA heading comment ${run}`;
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    await ensureDeliveryResources(page);

    const host = await createPage(page, `Comments Heading Host ${run}`);
    await placeWidget(page, 'percCommentsForm');
    await placeWidget(page, 'percComments');
    await saveLayout(page);
    await openContentTab(page);
    await widgetOnPage(page, 'percCommentsForm').click();
    await editorFrame(page).locator('img[title="Edit local content"]').first().click();
    const dialog = page.locator('.ui-dialog:visible', { hasText: 'Edit Widget Content' });
    await expect(page.frameLocator('iframe#edit-widget-content-frame').getByText('Comment box')).toBeVisible({ timeout: 30_000 });
    await dialog.getByRole('button', { name: 'Save' }).click();
    await expect(dialog).toBeHidden({ timeout: 30_000 });
    await openContentTab(page);
    await publishOpenItem(page);
    const editUrl = page.url();
    const url = `${DTS_URL}/${TEST_FOLDER}/${host}.html`;

    // one comment is enough: every format below is checked on the same comment
    await page.goto(url);
    const form = page.locator('.perc-comments-form form');
    await expect(form).toBeVisible({ timeout: 60_000 });
    await form.getByLabel('Title').fill(title);
    await form.getByLabel("User's name").fill(`QA Visitor ${run}`);
    await form.getByLabel('Comment box').fill(`Comment text ${run}`);
    await form.getByLabel('Email').fill(`qa${run}@example.com`);
    await form.getByRole('button', { name: 'Submit' }).click();
    await expect(page).toHaveURL(/lastCommentId=\d+/, { timeout: 30_000 });
    await expect(page.locator('.perc-comments-view .perc-comment-title'), 'default format: Heading 3').toHaveCount(1, { timeout: 60_000 });
    await expect(page.locator('.perc-comments-view h3.perc-comment-title'), 'the title is a Heading 3 by default').toHaveText(title);

    for (const tag of ['h1', 'p', 'div']) {
      await reopenPageInEditor(page, editUrl, host);
      const cfg = await openWidgetConfigure(page, 'percComments');
      await cfg.locator('select[name="titleFormat"]').selectOption(tag, { timeout: 10_000 });
      await page.screenshot({ path: `screenshots/comments-widget-title-format-${tag}-configure-dialog.png` });
      await confirmWidgetConfigure(page);
      await saveLayout(page);
      await openContentTab(page);
      await publishOpenItem(page);
      await page.goto(url);
      await expect(page.locator(`.perc-comments-view ${tag}.perc-comment-title`), `the title is written as ${tag}`).toHaveText(title, { timeout: 60_000 });
      await page.screenshot({ path: `screenshots/comments-widget-published-page-title-as-${tag}.png` });
    }
  });
});
