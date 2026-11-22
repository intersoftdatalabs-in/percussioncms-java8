import { test, expect, Page } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';
import { loginAsAdmin, createPage, publishOpenItem, configurePublishing } from '../helpers/cms-ui';
import { placeWidget, saveLayout, openContentTab, openWidgetContentEditor, openWidgetConfigure, confirmWidgetConfigure } from '../helpers/widgets-ui';
import { DTS_URL, DTS_ADMIN_URL, TEST_FOLDER } from '../helpers/qa-env';

/**
 * File widget: uploads a file and shows a link to it. The link is built by the CMS when the page is published, and the file itself is
 * delivered with the page, so the proof is the PUBLISHED page served by the DTS: the link, its attributes, and the bytes behind it.
 */
const PDF = path.join(__dirname, '..', 'fixtures', 'test.pdf');

/** Puts a File widget on a new page, optionally configures it, uploads the PDF with the given link text, publishes. Returns the published URL. */
async function publishFilePage(page: Page, run: string, linkText: string, shot: string, configure?: (form: ReturnType<Page['locator']>) => Promise<void>): Promise<string> {
  const host = await createPage(page, `File Host ${run}`);
  await placeWidget(page, 'percFile');
  if (configure) {
    const form = await openWidgetConfigure(page, 'percFile');
    await configure(form);
    await page.screenshot({ path: `screenshots/${shot}-configure-dialog.png` });
    await confirmWidgetConfigure(page);
  }
  await saveLayout(page);

  const { frame, save } = await openWidgetContentEditor(page, 'percFile');
  await frame.locator('#perc-content-edit-displaytitle').pressSequentially(linkText, { delay: 10 });
  await frame.locator('#perc-content-edit-item_file_attachment').setInputFiles(PDF);
  await page.screenshot({ path: `screenshots/${shot}-content-editor-file-chosen.png` });
  await save.click();
  await expect(save).toBeHidden({ timeout: 60_000 });
  await openContentTab(page);
  await publishOpenItem(page);
  return `${DTS_URL}/${TEST_FOLDER}/${host}.html`;
}

test.describe('File widget', () => {
  test.setTimeout(360_000);

  test('File widget: published page shows a link with the chosen text to the uploaded file, which the DTS serves unchanged', async ({ page }) => {
    const run = Date.now().toString().slice(-7);
    const linkText = `QA File ${run}`;
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishFilePage(page, run, linkText, 'file-widget-defaults');

    await page.goto(url);
    const link = page.locator('.perc-file a');
    await expect(link, 'link text on the published page').toHaveText(linkText);
    await expect(link).toHaveAttribute('title', linkText);
    await expect(link, 'opens in a new window by default').toHaveAttribute('target', '_blank');
    const href = (await link.getAttribute('href'))!;
    expect(href, 'link points to a PDF').toMatch(/\.pdf$/i);
    await page.screenshot({ path: 'screenshots/file-widget-published-page-shows-link-to-the-file.png' });

    // the file behind the link is the file that was uploaded
    const res = await page.request.get(new URL(href, page.url()).toString());
    expect(res.status(), `GET ${href}`).toBe(200);
    expect(res.headers()['content-type']).toContain('pdf');
    const body = await res.body();
    expect(body.length, 'delivered file size equals the uploaded file').toBe(fs.statSync(PDF).size);
    expect(body.subarray(0, 5).toString(), 'PDF signature').toBe('%PDF-');
    expect(Buffer.compare(body, fs.readFileSync(PDF)), 'delivered bytes equal the uploaded bytes').toBe(0);
  });

  test('File widget: link target and Referrer-Policy follow the options set in Configure', async ({ page }) => {
    const run = Date.now().toString().slice(-7);
    const linkText = `QA File Config ${run}`;
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishFilePage(page, run, linkText, 'file-widget-configured', async form => {
      await form.locator('input[name="linkNewWindow"]').uncheck();
      await form.locator('select[name="referalPolicy"]').selectOption('origin', { timeout: 10_000 });
    });

    await page.goto(url);
    const link = page.locator('.perc-file a');
    await expect(link).toHaveText(linkText);
    await expect(link, 'same window when "Open link in new window" is off').toHaveAttribute('target', '_self');
    await expect(link, 'Referrer-Policy chosen in Configure').toHaveAttribute('referrerpolicy', 'origin');
    await page.screenshot({ path: 'screenshots/file-widget-published-page-link-same-window-and-origin-policy.png' });
  });
});
