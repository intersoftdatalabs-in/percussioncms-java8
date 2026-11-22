import { test, expect, Page } from '@playwright/test';
import { loginAsAdmin, createPage, publishOpenItem, configurePublishing } from '../helpers/cms-ui';
import { placeWidget, saveLayout, openContentTab, openWidgetConfigure, confirmWidgetConfigure } from '../helpers/widgets-ui';
import { DTS_URL, DTS_ADMIN_URL, TEST_FOLDER } from '../helpers/qa-env';

/**
 * IFrame widget: embeds another web page in the page. It has no content of its own, everything comes from the Configure options
 * (URL, size, border, scrolling, margins, referrer policy, title / aria label, alternate content), so the proof is the iframe in the
 * PUBLISHED page served by the DTS: its attributes, and the page that really loads inside it.
 */
test.describe.serial('IFrame widget', () => {
  test.setTimeout(420_000);
  const run = Date.now().toString().slice(-7);
  let targetUrl = '';
  const targetTitle = `QA Frame Target ${run}`;

  /** Host page with an IFrame widget configured through the dialog, published. Returns the published URL. */
  async function publishFramePage(page: Page, shot: string, configure: (form: ReturnType<Page['locator']>) => Promise<void>): Promise<string> {
    const host = await createPage(page, `Frame Host ${shot.slice(-7)} ${run}`);
    await placeWidget(page, 'percIframe');
    const form = await openWidgetConfigure(page, 'percIframe');
    await configure(form);
    await page.screenshot({ path: `screenshots/${shot}-configure-dialog.png` });
    await confirmWidgetConfigure(page);
    await saveLayout(page);
    await openContentTab(page);
    await publishOpenItem(page);
    return `${DTS_URL}/${TEST_FOLDER}/${host}.html`;
  }

  test('IFrame widget: published page embeds the chosen URL with the title and size from Configure, and the page loads inside the frame', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    // the page that will be embedded: a plain page published to the same DTS
    const target = await createPage(page, targetTitle);
    await openContentTab(page);
    await publishOpenItem(page);
    targetUrl = `${DTS_URL}/${TEST_FOLDER}/${target}.html`;

    const url = await publishFramePage(page, 'iframe-widget-url-and-size', async form => {
      await form.locator('input[name="iframeurl"]').fill(targetUrl);
      await form.locator('input[name="title"]').fill(`QA frame ${run}`);
      await form.locator('input[name="height"]').fill('320');
      await form.locator('input[name="width"]').fill('60%');
    });

    await page.goto(url);
    const frameEl = page.locator('.perc-iframe iframe');
    await expect(frameEl, 'iframe in the published page').toHaveCount(1);
    await expect(frameEl).toHaveAttribute('src', targetUrl);
    await expect(frameEl).toHaveAttribute('title', `QA frame ${run}`);
    const style = (await frameEl.getAttribute('style'))!;
    expect(style, 'width from Configure').toContain('width: 60%');
    expect(style, 'height in pixels from Configure (a plain number gets px)').toContain('height: 320px');
    expect(style, 'border is off by default').toContain('border: 0');
    // the embedded page really loads in the frame
    const inner = page.frameLocator('.perc-iframe iframe');
    await expect(async () => {
      const child = page.frames().find(f => f.url() === targetUrl);
      expect(child, 'frame showing the embedded URL').toBeTruthy();
      expect(await child!.title(), 'document title of the embedded page').toContain(targetTitle);
    }).toPass({ timeout: 30_000 });
    void inner;
    await page.screenshot({ path: 'screenshots/iframe-widget-published-page-embeds-the-chosen-page.png' });
  });

  test('IFrame widget: border, scrolling, margins, referrer policy, aria label and alternate content from Configure are applied', async ({ page }) => {
    expect(targetUrl, 'target page from the first scenario').not.toBe('');
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishFramePage(page, 'iframe-widget-options', async form => {
      await form.locator('input[name="iframeurl"]').fill(targetUrl);
      await form.locator('input[name="height"]').fill('200');
      await form.locator('input[name="width"]').fill('300');
      await form.locator('input[name="marginheight"]').fill('10px');
      await form.locator('input[name="marginwidth"]').fill('20px');
      await form.locator('select[name="frameborder"]').selectOption('enable', { timeout: 10_000 });
      await form.locator('select[name="scrollbar"]').selectOption('no', { timeout: 10_000 });
      await form.locator('select[name="referalPolicy"]').selectOption('origin', { timeout: 10_000 });
      await form.locator('input[name="ariaLabel"]').fill(`QA aria ${run}`);
      await form.locator('input[name="alternateContent"]').fill(`QA alternate content ${run}`);
    });

    await page.goto(url);
    const frameEl = page.locator('.perc-iframe iframe');
    await expect(frameEl).toHaveCount(1);
    await expect(frameEl, 'Referrer-Policy from Configure').toHaveAttribute('referrerpolicy', 'origin');
    await expect(frameEl, 'aria label is used when there is no title').toHaveAttribute('aria-label', `QA aria ${run}`);
    const style = (await frameEl.getAttribute('style'))!;
    expect(style, 'width in pixels').toContain('width: 300px');
    expect(style, 'height in pixels').toContain('height: 200px');
    expect(style, 'scrolling "No" hides the overflow').toContain('overflow: hidden');
    expect(style, 'border enabled').toContain('border: 1');
    expect(style, 'top and bottom margin').toMatch(/margin-top: 10px;\s*margin-bottom: 10px/);
    expect(style, 'left and right margin').toMatch(/margin-left: 20px;\s*margin-right: 20px/);
    await expect(page.getByText(`QA alternate content ${run}`), 'alternate content is in the page').toHaveCount(1);
    await page.screenshot({ path: 'screenshots/iframe-widget-published-page-applies-configure-options.png' });
  });
});
