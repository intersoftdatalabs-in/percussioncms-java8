import { createHash } from 'crypto';
import { test, expect, Page } from '@playwright/test';
import { loginAsAdmin, createPage, publishOpenItem, configurePublishing } from '../helpers/cms-ui';
import { placeWidget, saveLayout, openContentTab, openWidgetConfigure, confirmWidgetConfigure } from '../helpers/widgets-ui';
import { DTS_URL, DTS_ADMIN_URL, TEST_FOLDER } from '../helpers/qa-env';

/**
 * jQuery Widget: has no content of its own, it chooses the jQuery script the page loads: its address, an optional jQuery Migrate script, whether
 * the tags are deferred, where in the page they go, and whether they are written to the published page at all. The proof is the script tags in
 * the PUBLISHED page served by the DTS, and jQuery really running in the browser.
 */
const JQUERY = '/web_resources/cm/jslib/jquery.js';
const MIGRATE = '/web_resources/cm/jslib/jquery-migrate.js';

/** New page with a jQuery Widget configured through the dialog (when `configure` is given), published. Returns the published URL. */
async function publishJQueryPage(page: Page, run: string, shot: string, configure?: (form: ReturnType<Page['locator']>) => Promise<void>): Promise<string> {
  const host = await createPage(page, `jQuery Host ${shot.slice(-9)} ${run}`);
  await placeWidget(page, 'percJQueryWidget');
  const form = await openWidgetConfigure(page, 'percJQueryWidget');
  if (configure) await configure(form);
  await page.screenshot({ path: `screenshots/${shot}-configure-dialog.png` });
  await confirmWidgetConfigure(page);
  await saveLayout(page);
  await openContentTab(page);
  await publishOpenItem(page);
  return `${DTS_URL}/${TEST_FOLDER}/${host}.html`;
}

/** The jQuery / Migrate script tags of the page in document order: address, whether in head or body, whether deferred. */
const jqueryTags = (page: Page) => page.evaluate(() => Array.from(document.querySelectorAll('script[src]'))
  .filter(s => /\/jquery(-migrate)?\.js$/.test(s.getAttribute('src') || ''))
  .map(s => ({ src: s.getAttribute('src')!, where: s.closest('head') ? 'head' : 'body', defer: s.hasAttribute('defer') })));

const runtime = (page: Page) => page.evaluate(() => ({ jq: (window as any).jQuery?.fn?.jquery as string | undefined, migrate: (window as any).jQuery?.migrateVersion as string | undefined }));

test.describe('jQuery Widget', () => {
  test.setTimeout(360_000);
  const run = Date.now().toString().slice(-7);

  test('jQuery Widget: with the default options the jQuery script is in the head, not deferred, without Migrate, and jQuery runs', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishJQueryPage(page, run, 'jquery-widget-defaults');
    await page.goto(url);
    expect(await jqueryTags(page), 'one jQuery tag in the head, plain').toEqual([{ src: JQUERY, where: 'head', defer: false }]);
    const rt = await runtime(page);
    expect(rt.jq, 'jQuery 3 is running').toMatch(/^3\./);
    expect(rt.migrate, 'no Migrate').toBeUndefined();
    await page.screenshot({ path: 'screenshots/jquery-widget-published-page-defaults.png' });
  });

  test('jQuery Widget: script location, defer and jQuery Migrate from Configure are written to the published page and run', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishJQueryPage(page, run, 'jquery-widget-bodyclose-defer-migrate', async form => {
      await form.locator('input[name="jQueryLocation"]').fill(JQUERY);
      await form.locator('input[name="jQueryMigrateLocation"]').fill(MIGRATE);
      await form.locator('select[name="isDeferred"]').selectOption('yes', { timeout: 10_000 });
      await form.locator('select[name="scriptLocation"]').selectOption('beforeBodyClose', { timeout: 10_000 });
    });
    await page.goto(url);
    expect(await jqueryTags(page), 'jQuery then Migrate, at the end of the body, both deferred').toEqual([
      { src: JQUERY, where: 'body', defer: true },
      { src: MIGRATE, where: 'body', defer: true },
    ]);
    const rt = await runtime(page);
    expect(rt.jq, 'jQuery 3 is running').toMatch(/^3\./);
    expect(rt.migrate, 'jQuery Migrate is running').toMatch(/^3\./);
    await page.screenshot({ path: 'screenshots/jquery-widget-published-page-body-close-deferred-with-migrate.png' });
  });

  test('jQuery Widget: "After Body Start" puts the script in the body', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishJQueryPage(page, run, 'jquery-widget-after-body-start', async form => {
      await form.locator('select[name="scriptLocation"]').selectOption('afterBodyStart', { timeout: 10_000 });
    });
    await page.goto(url);
    expect(await jqueryTags(page), 'jQuery in the body, not deferred').toEqual([{ src: JQUERY, where: 'body', defer: false }]);
    expect((await runtime(page)).jq, 'jQuery runs').toMatch(/^3\./);
  });

  test('jQuery Widget: "Include on Published Page" = No leaves the jQuery script out of the published page', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishJQueryPage(page, run, 'jquery-widget-not-included', async form => {
      await form.locator('select[name="includeOnPublishedPage"]').selectOption('no', { timeout: 10_000 });
    });
    await page.goto(url);
    expect(await jqueryTags(page), 'no jQuery script on the page').toEqual([]);
    expect((await runtime(page)).jq, 'jQuery is not loaded').toBeUndefined();
    await page.screenshot({ path: 'screenshots/jquery-widget-published-page-jquery-not-included.png' });
  });

  // ---- scripts from a CDN (needs internet access) ----
  const CDN_JQUERY = 'https://code.jquery.com/jquery-3.7.1.min.js';
  const CDN_MIGRATE = 'https://code.jquery.com/jquery-migrate-3.4.1.min.js';

  /** SRI value (sha384) of the file at `url`, computed from the downloaded bytes. */
  const sri = async (page: Page, url: string) => {
    const res = await page.request.get(url);
    expect(res.status(), `download ${url}`).toBe(200);
    return 'sha384-' + createHash('sha384').update(await res.body()).digest('base64');
  };
  const tagsWithIntegrity = (page: Page) => page.evaluate(() => Array.from(document.querySelectorAll('script[src^="https://code.jquery.com/"]'))
    .map(s => ({ src: s.getAttribute('src')!, integrity: s.getAttribute('integrity'), crossorigin: s.getAttribute('crossorigin'), defer: s.hasAttribute('defer') })));

  test('jQuery Widget: CDN addresses with SRI hash and cross-origin setting are written as given and the browser loads them', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const jqHash = await sri(page, CDN_JQUERY);
    const migHash = await sri(page, CDN_MIGRATE);
    const url = await publishJQueryPage(page, run, 'jquery-widget-cdn-sri', async form => {
      await form.locator('input[name="jQueryLocation"]').fill(CDN_JQUERY);
      await form.locator('input[name="jQueryLocationIntegrity"]').fill(jqHash);
      await form.locator('input[name="jQueryLocationCrossOrigin"]').fill('anonymous');
      await form.locator('input[name="jQueryMigrateLocation"]').fill(CDN_MIGRATE);
      await form.locator('input[name="jQueryMigrateLocationIntegrity"]').fill(migHash);
      await form.locator('input[name="jQueryMigrateLocationCrossOrigin"]').fill('anonymous');
    });
    await page.goto(url);
    expect(await tagsWithIntegrity(page), 'CDN tags with their SRI hash and cross-origin value').toEqual([
      { src: CDN_JQUERY, integrity: jqHash, crossorigin: 'anonymous', defer: false },
      { src: CDN_MIGRATE, integrity: migHash, crossorigin: 'anonymous', defer: false },
    ]);
    const rt = await runtime(page);
    expect(rt.jq, 'the CDN jQuery (3.7.1), not the system one (3.6.0), is running').toBe('3.7.1');
    expect(rt.migrate, 'the CDN Migrate (3.4.1) is running').toBe('3.4.1');
    await page.screenshot({ path: 'screenshots/jquery-widget-published-page-cdn-scripts-with-sri.png' });
  });

  test('jQuery Widget: a wrong SRI hash is written to the tag and the browser refuses to run the script', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const wrong = 'sha384-' + Buffer.alloc(48, 7).toString('base64');
    const url = await publishJQueryPage(page, run, 'jquery-widget-cdn-wrong-sri', async form => {
      await form.locator('input[name="jQueryLocation"]').fill(CDN_JQUERY);
      await form.locator('input[name="jQueryLocationIntegrity"]').fill(wrong);
      await form.locator('input[name="jQueryLocationCrossOrigin"]').fill('anonymous');
    });
    const blocked: string[] = [];
    page.on('console', m => { if (/integrity|digest/i.test(m.text())) blocked.push(m.text()); });
    await page.goto(url);
    expect((await tagsWithIntegrity(page))[0], 'tag carries the wrong hash as typed').toMatchObject({ src: CDN_JQUERY, integrity: wrong, crossorigin: 'anonymous' });
    await expect.poll(() => blocked.length, { message: 'the browser reports the integrity failure', timeout: 15_000 }).toBeGreaterThan(0);
    expect((await runtime(page)).jq, 'jQuery from the CDN did not run').toBeUndefined();
  });

  test('jQuery Widget: an address that cannot be reached falls back to the system jQuery', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const missing = `https://code.jquery.com/does-not-exist-${run}.js`;
    const url = await publishJQueryPage(page, run, 'jquery-widget-cdn-unreachable', async form => {
      await form.locator('input[name="jQueryLocation"]').fill(missing);
    });
    const html = await (await page.request.get(url)).text();
    expect(html, 'comment left by the CMS').toContain('Unable to verify CDN link - defaulting to system jquery version');
    expect(html, 'the unreachable address is not written as a script').not.toContain(missing);
    await page.goto(url);
    expect(await jqueryTags(page), 'the system jquery.js is used instead').toEqual([{ src: JQUERY, where: 'head', defer: false }]);
    expect((await runtime(page)).jq, 'system jQuery 3.6.0 runs').toBe('3.6.0');
    await page.screenshot({ path: 'screenshots/jquery-widget-published-page-unreachable-address-falls-back.png' });
  });
});
