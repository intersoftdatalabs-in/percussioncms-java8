import { createHash } from 'crypto';
import { test, expect, Page } from '@playwright/test';
import { loginAsAdmin, createPage, publishOpenItem, configurePublishing, ensureDeliveryResources } from '../helpers/cms-ui';
import { placeWidget, saveLayout, openContentTab, openWidgetConfigure, confirmWidgetConfigure } from '../helpers/widgets-ui';
import { DTS_URL, DTS_ADMIN_URL, TEST_FOLDER } from '../helpers/qa-env';

/**
 * jQuery-ui Widget: has no content of its own, it chooses the jQuery UI script and the jQuery UI style sheet the page loads: the script address
 * (with optional SRI hash and cross-origin value), the style sheet, whether the script tag is deferred, where in the page it goes, and whether
 * they are written to the published page at all. The proof is the script and link tags in the PUBLISHED page served by the DTS, and jQuery UI
 * really running in the browser.
 */
const UI = '/web_resources/cm/jslib/jquery-ui.js';
const THEME = '/web_resources/cm/themes/smoothness/jquery-ui-1.8.9.custom.css';

/** New page with a jQuery UI Widget configured through the dialog (when `configure` is given), published. Returns the published URL. */
async function publishUiPage(page: Page, run: string, shot: string, configure?: (form: ReturnType<Page['locator']>) => Promise<void>): Promise<string> {
  const host = await createPage(page, `jQuery UI Host ${shot.slice(-9)} ${run}`);
  await placeWidget(page, 'percJQueryUIWidget');
  const form = await openWidgetConfigure(page, 'percJQueryUIWidget');
  if (configure) await configure(form);
  await page.screenshot({ path: `screenshots/${shot}-configure-dialog.png` });
  await confirmWidgetConfigure(page);
  await saveLayout(page);
  await openContentTab(page);
  await publishOpenItem(page);
  return `${DTS_URL}/${TEST_FOLDER}/${host}.html`;
}

/** The jQuery UI script tags of the page in document order: address, whether in head or body, whether deferred. */
const uiTags = (page: Page) => page.evaluate(() => Array.from(document.querySelectorAll('script[src]'))
  .filter(s => /\/jquery-ui(\.min)?\.js$/.test(s.getAttribute('src') || ''))
  .map(s => ({ src: s.getAttribute('src')!, where: s.closest('head') ? 'head' : 'body', defer: s.hasAttribute('defer') })));

/** The jQuery UI style sheet links of the page (address only). */
const uiCss = (page: Page) => page.evaluate(() => Array.from(document.querySelectorAll('link[rel="stylesheet"]'))
  .map(l => l.getAttribute('href')!).filter(h => /jquery-ui/.test(h)));

const runtime = (page: Page) => page.evaluate(() => {
  const jq = (window as any).jQuery;
  return { ui: jq?.ui?.version as string | undefined, dialog: typeof jq?.fn?.dialog as string | undefined };
});

test.describe('jQuery-ui Widget', () => {
  test.setTimeout(360_000);
  const run = Date.now().toString().slice(-7);

  test('jQuery-ui Widget: with the default options the jQuery UI script is in the head, not deferred, with the default style sheet, and jQuery UI runs', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    await ensureDeliveryResources(page);
    const url = await publishUiPage(page, run, 'jquery-ui-widget-defaults');
    await page.goto(url);
    expect(await uiTags(page), 'one jQuery UI tag in the head, plain').toEqual([{ src: UI, where: 'head', defer: false }]);
    expect(await uiCss(page), 'the default style sheet').toEqual([THEME]);
    const rt = await runtime(page);
    expect(rt.ui, 'jQuery UI is running').toMatch(/^1\.\d+\.\d+$/);
    expect(rt.dialog, 'a jQuery UI plug-in (dialog) is available').toBe('function');
    await page.screenshot({ path: 'screenshots/jquery-ui-widget-published-page-defaults.png' });
  });

  test('jQuery-ui Widget: script location "Before Body Close" and Defer Loading "Yes" from Configure are written to the published page and run', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    await ensureDeliveryResources(page);
    const url = await publishUiPage(page, run, 'jquery-ui-widget-bodyclose-defer', async form => {
      await form.locator('select[name="isUIDeferred"]').selectOption('yes', { timeout: 10_000 });
      await form.locator('select[name="scriptUILocation"]').selectOption('beforeBodyClose', { timeout: 10_000 });
    });
    await page.goto(url);
    expect(await uiTags(page), 'at the end of the body, deferred').toEqual([{ src: UI, where: 'body', defer: true }]);
    expect((await runtime(page)).ui, 'jQuery UI runs').toMatch(/^1\./);
    await page.screenshot({ path: 'screenshots/jquery-ui-widget-published-page-body-close-deferred.png' });
  });

  test('jQuery-ui Widget: script location "After Body Start" puts the script in the body', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    await ensureDeliveryResources(page);
    const url = await publishUiPage(page, run, 'jquery-ui-widget-after-body-start', async form => {
      await form.locator('select[name="scriptUILocation"]').selectOption('afterBodyStart', { timeout: 10_000 });
    });
    await page.goto(url);
    expect(await uiTags(page), 'in the body, not deferred').toEqual([{ src: UI, where: 'body', defer: false }]);
    expect((await runtime(page)).ui, 'jQuery UI runs').toMatch(/^1\./);
  });

  test('jQuery-ui Widget: "Include on Published Page" = No leaves the jQuery UI script and style sheet out of the published page', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    await ensureDeliveryResources(page);
    const url = await publishUiPage(page, run, 'jquery-ui-widget-not-included', async form => {
      await form.locator('select[name="includeUIOnPublishedPage"]').selectOption('no', { timeout: 10_000 });
    });
    await page.goto(url);
    expect(await uiTags(page), 'no jQuery UI script on the page').toEqual([]);
    expect(await uiCss(page), 'no jQuery UI style sheet on the page').toEqual([]);
    expect((await runtime(page)).ui, 'jQuery UI is not loaded').toBeUndefined();
    await page.screenshot({ path: 'screenshots/jquery-ui-widget-published-page-jquery-ui-not-included.png' });
  });

  // ---- scripts and style sheets from a CDN (needs internet access) ----
  const CDN_UI = 'https://code.jquery.com/ui/1.12.1/jquery-ui.min.js';
  const CDN_CSS = 'https://code.jquery.com/ui/1.12.1/themes/base/jquery-ui.css';

  /** Skips the scenario when code.jquery.com cannot be reached, so an offline QA machine does not report false failures. */
  const requireInternet = async (page: Page) => {
    const ok = await page.request.get(CDN_UI, { timeout: 15_000 }).then(r => r.ok(), () => false);
    test.skip(!ok, 'needs internet access (code.jquery.com)');
  };

  /** SRI value (sha384) of the file at `url`, computed from the downloaded bytes. */
  const sri = async (page: Page, url: string) => {
    const res = await page.request.get(url);
    expect(res.status(), `download ${url}`).toBe(200);
    return 'sha384-' + createHash('sha384').update(await res.body()).digest('base64');
  };
  const cdnTags = (page: Page) => page.evaluate(() => Array.from(document.querySelectorAll('script[src^="https://code.jquery.com/"]'))
    .map(s => ({ src: s.getAttribute('src')!, integrity: s.getAttribute('integrity'), crossorigin: s.getAttribute('crossorigin'), defer: s.hasAttribute('defer') })));

  test('jQuery-ui Widget: a CDN address with SRI hash and cross-origin setting and a CDN style sheet are written as given and the browser loads them', async ({ page }) => {
    await requireInternet(page);
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    await ensureDeliveryResources(page);
    const hash = await sri(page, CDN_UI);
    const url = await publishUiPage(page, run, 'jquery-ui-widget-cdn-sri', async form => {
      await form.locator('input[name="jQueryUILocation"]').fill(CDN_UI);
      await form.locator('input[name="jQueryUILocationIntegrity"]').fill(hash);
      await form.locator('input[name="jQueryUILocationCrossOrigin"]').fill('anonymous');
      await form.locator('input[name="jQueryUICSSTheme"]').fill(CDN_CSS);
    });
    await page.goto(url);
    expect(await cdnTags(page), 'CDN tag with its SRI hash and cross-origin value').toEqual([{ src: CDN_UI, integrity: hash, crossorigin: 'anonymous', defer: false }]);
    expect(await uiCss(page), 'the chosen style sheet replaces the default one').toEqual([CDN_CSS]);
    const rt = await runtime(page);
    expect(rt.ui, 'the CDN jQuery UI (1.12.1), not the system one (1.13.2), is running').toBe('1.12.1');
    await page.screenshot({ path: 'screenshots/jquery-ui-widget-published-page-cdn-script-and-style-sheet.png' });
  });

  test('jQuery-ui Widget: a wrong SRI hash is written to the tag and the browser refuses to run the script', async ({ page }) => {
    await requireInternet(page);
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    await ensureDeliveryResources(page);
    const wrong = 'sha384-' + Buffer.alloc(48, 7).toString('base64');
    const url = await publishUiPage(page, run, 'jquery-ui-widget-cdn-wrong-sri', async form => {
      await form.locator('input[name="jQueryUILocation"]').fill(CDN_UI);
      await form.locator('input[name="jQueryUILocationIntegrity"]').fill(wrong);
      await form.locator('input[name="jQueryUILocationCrossOrigin"]').fill('anonymous');
    });
    const blocked: string[] = [];
    page.on('console', m => { if (/integrity|digest/i.test(m.text())) blocked.push(m.text()); });
    await page.goto(url);
    expect((await cdnTags(page))[0], 'tag carries the wrong hash as typed').toMatchObject({ src: CDN_UI, integrity: wrong, crossorigin: 'anonymous' });
    await expect.poll(() => blocked.length, { message: 'the browser reports the integrity failure', timeout: 15_000 }).toBeGreaterThan(0);
    expect((await runtime(page)).ui, 'jQuery UI from the CDN did not run').toBeUndefined();
  });

  test('jQuery-ui Widget: an address that cannot be reached falls back to the system jQuery UI', async ({ page }) => {
    await requireInternet(page);
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    await ensureDeliveryResources(page);
    const missing = `https://code.jquery.com/ui/does-not-exist-${run}.js`;
    const url = await publishUiPage(page, run, 'jquery-ui-widget-cdn-unreachable', async form => {
      await form.locator('input[name="jQueryUILocation"]').fill(missing);
    });
    const html = await (await page.request.get(url)).text();
    expect(html, 'comment left by the CMS').toContain('Unable to verify CDN link - defaulting to system jquery version');
    expect(html, 'the unreachable address is not written as a script').not.toContain(missing);
    await page.goto(url);
    expect(await uiTags(page), 'the system jquery-ui.js is used instead').toEqual([{ src: UI, where: 'head', defer: false }]);
    expect((await runtime(page)).ui, 'system jQuery UI runs').toMatch(/^1\./);
    await page.screenshot({ path: 'screenshots/jquery-ui-widget-published-page-unreachable-address-falls-back.png' });
  });
});
