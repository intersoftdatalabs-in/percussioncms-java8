import { test, expect } from '@playwright/test';
import { loginAsAdmin, createPage, publishOpenItem, configurePublishing } from '../helpers/cms-ui';
import { placeWidget, saveLayout, openContentTab, widgetOnPage, editorFrame } from '../helpers/widgets-ui';
import { DTS_URL, DTS_ADMIN_URL, TEST_FOLDER } from '../helpers/qa-env';

/**
 * HTML widget: takes raw HTML typed in a code editor and writes it into the page as it is (not escaped, scripts included). The proof is the
 * PUBLISHED page served by the DTS: the markup is rendered, the link keeps its address and the script runs.
 */
test.describe('HTML widget', () => {
  test.setTimeout(360_000);

  test('HTML widget: raw HTML typed in the editor is rendered as markup on the published page, link and script included', async ({ page }) => {
    const run = Date.now().toString().slice(-7);
    const html =
      `<div id="qa-html-box" style="color: rgb(0, 128, 0)"><h2 class="qa-heading">QA HTML heading ${run}</h2>` +
      `<p class="qa-para">Paragraph ${run} with <a class="qa-link" href="https://example.com/qa-${run}" target="_blank">a link</a></p>` +
      `<span id="qa-script-out"></span><script>document.getElementById('qa-script-out').textContent = 'script ran ${run}';</script></div>`;

    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const host = await createPage(page, `Html Host ${run}`);
    await placeWidget(page, 'percRawHtml');
    await saveLayout(page);

    // the content editor is a code editor (no text box), so it is opened and filled by hand
    await openContentTab(page);
    await widgetOnPage(page, 'percRawHtml').click();
    await editorFrame(page).locator('img[title="Edit local content"]').first().click();
    const dialog = page.locator('.ui-dialog:visible', { hasText: 'Edit Widget Content' });
    const editor = page.frameLocator('iframe#edit-widget-content-frame');
    await expect(editor.getByText('HTML:')).toBeVisible({ timeout: 30_000 });
    await editor.locator('.CodeMirror, .ace_editor, .cm-editor').first().click({ timeout: 15_000 });
    await page.keyboard.type(html, { delay: 5 });
    await page.screenshot({ path: 'screenshots/html-widget-content-editor-raw-html-typed.png' });
    await dialog.getByRole('button', { name: 'Save' }).click();
    await expect(dialog).toBeHidden({ timeout: 60_000 });
    await openContentTab(page);
    await publishOpenItem(page);

    await page.goto(`${DTS_URL}/${TEST_FOLDER}/${host}.html`);
    const box = page.locator('#qa-html-box');
    await expect(box.locator('h2.qa-heading'), 'heading rendered as markup, not as text').toHaveText(`QA HTML heading ${run}`);
    await expect(box.locator('a.qa-link'), 'link text').toHaveText('a link');
    await expect(box.locator('a.qa-link'), 'link address is kept').toHaveAttribute('href', `https://example.com/qa-${run}`);
    await expect(box.locator('a.qa-link')).toHaveAttribute('target', '_blank');
    await expect(box, 'inline style is applied').toHaveCSS('color', 'rgb(0, 128, 0)');
    await expect(page.locator('#qa-script-out'), 'the script in the widget ran').toHaveText(`script ran ${run}`);
    await expect(page.getByText('<h2'), 'markup is not shown as text').toHaveCount(0);
    await page.screenshot({ path: 'screenshots/html-widget-published-page-renders-raw-html-and-runs-script.png' });
  });
});
