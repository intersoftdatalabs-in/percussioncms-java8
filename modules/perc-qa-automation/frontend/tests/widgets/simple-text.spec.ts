import { test, expect, Page, FrameLocator } from '@playwright/test';
import { loginAsAdmin, createPage, publishOpenItem, configurePublishing, ensureDeliveryResources, reopenPageInEditor } from '../helpers/cms-ui';
import { placeWidget, saveLayout, openContentTab, widgetOnPage, editorFrame } from '../helpers/widgets-ui';
import { DTS_URL, DTS_ADMIN_URL, TEST_FOLDER } from '../helpers/qa-env';

/**
 * Simple Text: a small editor (TinyMCE with bold, italic, underline, undo, redo and a styles menu, no menu bar) for short formatted text. The proof
 * is the PUBLISHED page served by the DTS: the formatting chosen in the editor is in the page as markup, and the saved text comes back when the editor
 * is opened again.
 */
const run = Date.now().toString().slice(-7);

async function openEditor(page: Page) {
  await openContentTab(page);
  await widgetOnPage(page, 'percSimpleText').click();
  await editorFrame(page).locator('img[title="Edit local content"]').first().click();
  const dialog = page.locator('.ui-dialog:visible', { hasText: 'Edit Widget Content' });
  const frame = page.frameLocator('iframe#edit-widget-content-frame');
  await expect(frame.locator('.tox-tinymce'), 'the editor is shown').toBeVisible({ timeout: 60_000 });
  return { frame, dialog, save: dialog.getByRole('button', { name: 'Save' }) };
}
const body = (frame: FrameLocator) => frame.frameLocator('.tox-edit-area__iframe').locator('body');

test.describe('Simple Text', () => {
  test.setTimeout(150_000);

  test('Simple Text: bold, italic, underline, a second paragraph and a heading chosen in the editor are published as markup and kept when the editor is opened again', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    await ensureDeliveryResources(page);
    const host = await createPage(page, `Simple Host ${run}`);
    await placeWidget(page, 'percSimpleText');
    await saveLayout(page);

    const { frame, dialog, save } = await openEditor(page);
    await expect(frame.getByRole('button', { name: 'Bold' }), 'toolbar has Bold').toBeVisible();
    await expect(frame.getByRole('button', { name: 'Italic' }), 'toolbar has Italic').toBeVisible();
    await expect(frame.getByRole('button', { name: 'Underline' }), 'toolbar has Underline').toBeVisible();
    await expect(frame.locator('.tox-menubar:visible'), 'no menu bar is shown').toHaveCount(0);
    const text = body(frame);
    await text.click();
    await page.keyboard.type(`Plain ${run} `);
    await frame.getByRole('button', { name: 'Bold' }).click();
    await page.keyboard.type(`bold ${run} `);
    await frame.getByRole('button', { name: 'Bold' }).click();
    await frame.getByRole('button', { name: 'Italic' }).click();
    await page.keyboard.type(`italic ${run} `);
    await frame.getByRole('button', { name: 'Italic' }).click();
    await frame.getByRole('button', { name: 'Underline' }).click();
    await page.keyboard.type(`underlined ${run}`);
    await frame.getByRole('button', { name: 'Underline' }).click();
    await page.keyboard.press('Enter');
    await page.keyboard.type(`Second paragraph ${run}`);
    await page.keyboard.press('Enter');
    await frame.getByRole('button', { name: /Paragraph|Formats/ }).click();
    await frame.getByRole('menuitem', { name: 'Headings' }).click();
    await frame.getByRole('menuitemcheckbox', { name: 'Heading 2' }).click();
    await page.keyboard.type(`Heading ${run}`);
    await expect(text.locator('h2'), 'heading shown in the editor').toHaveText(`Heading ${run}`);
    await page.screenshot({ path: 'screenshots/simple-text-content-editor-formatted.png' });
    await save.click();
    await expect(dialog).toBeHidden({ timeout: 60_000 });
    await openContentTab(page);
    await publishOpenItem(page);
    const editUrl = page.url();

    // the saved text comes back when the editor is opened again
    await reopenPageInEditor(page, editUrl, host);
    const again = await openEditor(page);
    await expect(body(again.frame).locator('h2'), 'heading kept').toHaveText(`Heading ${run}`);
    await expect(body(again.frame).locator('strong'), 'bold kept').toHaveText(`bold ${run}`);
    await again.dialog.getByRole('button', { name: 'Cancel' }).click();
    await expect(again.dialog).toBeHidden({ timeout: 30_000 });

    await page.goto(`${DTS_URL}/${TEST_FOLDER}/${host}.html`);
    await expect(page.locator('strong', { hasText: `bold ${run}` }), 'bold').toBeVisible();
    await expect(page.locator('em', { hasText: `italic ${run}` }), 'italic').toBeVisible();
    await expect(page.locator('span[style*="underline"], u', { hasText: `underlined ${run}` }).first(), 'underline').toBeVisible();
    await expect(page.locator('p', { hasText: `Second paragraph ${run}` }), 'second paragraph').toBeVisible();
    await expect(page.locator('h2', { hasText: `Heading ${run}` }), 'heading').toBeVisible();
    await page.screenshot({ path: 'screenshots/simple-text-published-page-formatted.png' });
  });
});
