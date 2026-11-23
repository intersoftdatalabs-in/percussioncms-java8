import { test, expect, Page, FrameLocator } from '@playwright/test';
import { loginAsAdmin, createPage, publishOpenItem, configurePublishing, ensureDeliveryResources, reopenPageInEditor } from '../helpers/cms-ui';
import { placeWidget, saveLayout, openContentTab, widgetOnPage, editorFrame } from '../helpers/widgets-ui';
import { DTS_URL, DTS_ADMIN_URL, TEST_FOLDER } from '../helpers/qa-env';

/**
 * Rich Text: text written in a WYSIWYG editor (TinyMCE) and written into the page as the HTML it produces. The proof is the PUBLISHED page served by
 * the DTS: bold text, a list, a heading, a link and a table typed or pasted as source are all rendered as markup, and the text is kept when the
 * editor is opened again.
 */
process.env.QA_RICH_RUN ??= Date.now().toString().slice(-7);
const run = process.env.QA_RICH_RUN;

/** Opens the editor of the Rich Text widget on the page (the content tab). Returns the editor frame and the dialog's Save button. */
async function openRichEditor(page: Page) {
  await openContentTab(page);
  await widgetOnPage(page, 'percRichText').click();
  await editorFrame(page).locator('img[title="Edit local content"]').first().click();
  const dialog = page.locator('.ui-dialog:visible', { hasText: 'Edit Widget Content' });
  const frame = page.frameLocator('iframe#edit-widget-content-frame');
  await expect(frame.locator('.tox-tinymce'), 'the editor is shown').toBeVisible({ timeout: 60_000 });
  return { frame, dialog, save: dialog.getByRole('button', { name: 'Save' }) };
}

/** The typing area of the editor (an iframe inside the editor frame). */
const body = (frame: FrameLocator) => frame.frameLocator('.tox-edit-area__iframe').locator('body');

/** Replaces the whole content through the editor's "Source code" dialog. */
async function setSource(page: Page, frame: FrameLocator, html: string) {
  await frame.getByRole('button', { name: 'Source code' }).click();
  const dlg = frame.locator('.tox-dialog');
  await expect(dlg).toBeVisible({ timeout: 15_000 });
  // the source dialog is a page (codemirror/source.html) in its own frame holding a code editor, not a text box
  await dlg.frameLocator('iframe').locator('.CodeMirror').first().click({ timeout: 15_000 });
  await page.keyboard.press('Control+a');
  await page.keyboard.insertText(html);
  await dlg.getByRole('button', { name: 'Ok' }).click();
  await expect(dlg).toBeHidden({ timeout: 15_000 });
}

test.describe.serial('Rich Text', () => {
  test.setTimeout(240_000);
  let host = '';
  let editUrl = '';   // the page in the CMS editor (the finder lists only the first 100 of the many test pages)

  test('Rich Text: text typed in the editor with bold and a bullet list is published as that markup', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    await ensureDeliveryResources(page);
    host = await createPage(page, `Rich Host ${run}`);
    await placeWidget(page, 'percRichText');
    await saveLayout(page);

    const { frame, dialog, save } = await openRichEditor(page);
    const text = body(frame);
    await text.click();
    await page.keyboard.type(`Plain words ${run} `, { delay: 10 });
    await page.keyboard.press('Control+b');
    await page.keyboard.type(`bold words ${run}`, { delay: 10 });
    await page.keyboard.press('Control+b');
    await page.keyboard.press('Enter');
    await frame.getByRole('button', { name: 'Bullet list' }).click();
    await page.keyboard.type(`First point ${run}`, { delay: 10 });
    await page.keyboard.press('Enter');
    await page.keyboard.type(`Second point ${run}`, { delay: 10 });
    await expect(text.locator('strong'), 'bold shown in the editor').toContainText(`bold words ${run}`);
    await page.screenshot({ path: 'screenshots/rich-text-content-editor-bold-and-list.png' });
    await save.click();
    await expect(dialog).toBeHidden({ timeout: 60_000 });
    await openContentTab(page);
    await publishOpenItem(page);
    editUrl = page.url();

    await page.goto(`${DTS_URL}/${TEST_FOLDER}/${host}.html`);
    await expect(page.locator('strong', { hasText: `bold words ${run}` }), 'bold text is published as strong').toBeVisible();
    await expect(page.getByText(`Plain words ${run}`), 'plain text is published').toBeVisible();
    await expect(page.locator('ul > li'), 'list items').toHaveText([`First point ${run}`, `Second point ${run}`]);
    await page.screenshot({ path: 'screenshots/rich-text-published-page-bold-and-list.png' });
  });

  test('Rich Text: HTML set through "Source code" (heading, link, table) is published as markup and is kept when the editor is opened again', async ({ page }) => {
    expect(editUrl, 'page from the first scenario').not.toBe('');
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const html =
      `<h2 id="qa-rich-h2">Rich heading ${run}</h2>` +
      `<p>Paragraph with <a id="qa-rich-link" href="https://example.com/rich-${run}" target="_blank">a link</a></p>` +
      `<table id="qa-rich-table" border="1"><tbody><tr><td>Cell A ${run}</td><td>Cell B ${run}</td></tr></tbody></table>`;
    await reopenPageInEditor(page, editUrl, host);
    const { frame, dialog, save } = await openRichEditor(page);
    await setSource(page, frame, html);
    await expect(body(frame).locator('h2'), 'heading shown in the editor').toHaveText(`Rich heading ${run}`);
    await page.screenshot({ path: 'screenshots/rich-text-content-editor-source-code-heading-link-table.png' });
    await save.click();
    await expect(dialog).toBeHidden({ timeout: 60_000 });
    await openContentTab(page);
    await publishOpenItem(page);

    await page.goto(`${DTS_URL}/${TEST_FOLDER}/${host}.html`);
    await expect(page.locator('h2#qa-rich-h2'), 'heading rendered').toHaveText(`Rich heading ${run}`);
    await expect(page.locator('a#qa-rich-link'), 'link address kept').toHaveAttribute('href', `https://example.com/rich-${run}`);
    await expect(page.locator('a#qa-rich-link')).toHaveAttribute('target', '_blank');
    await expect(page.locator('#qa-rich-table td'), 'table cells').toHaveText([`Cell A ${run}`, `Cell B ${run}`]);
    await expect(page.getByText('<h2'), 'markup is not shown as text').toHaveCount(0);
    await page.screenshot({ path: 'screenshots/rich-text-published-page-heading-link-table.png' });

    // the saved text comes back when the editor is opened again
    await reopenPageInEditor(page, editUrl, host);
    const again = await openRichEditor(page);
    await expect(body(again.frame).locator('h2'), 'the text is kept').toHaveText(`Rich heading ${run}`);
    await again.dialog.getByRole('button', { name: 'Cancel' }).click();
  });
});
