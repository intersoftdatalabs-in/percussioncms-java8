import { test, expect, Page, FrameLocator } from '@playwright/test';
import { loginAsAdmin, createPage, publishOpenItem, configurePublishing } from '../helpers/cms-ui';
import { placeWidget, saveLayout, openContentTab, openWidgetContentEditor } from '../helpers/widgets-ui';
import { DTS_URL, DTS_ADMIN_URL, TEST_FOLDER, TEST_SITE } from '../helpers/qa-env';

/**
 * Default Language: for international sites. A table in the widget's editor lists the sites, languages and regions of a page that exist in several sites; the
 * widget writes a <link rel="alternate" hreflang="..."> for each row into the head of the published page (and x-default for the default row). The widget only
 * writes a row when the same page (same folder path, same title) exists in the site of that row: the QA install has one site, so the rows use that site
 * (every alternate then points at the page itself, which is enough to see what the widget writes).
 */
process.env.QA_DEFLANG_RUN ??= Date.now().toString().slice(-7);   // shared with the workers Playwright restarts after a failed scenario
const run = process.env.QA_DEFLANG_RUN;

type Row = { protocol: 'http' | 'https'; language: string; country?: string; isDefault?: boolean };

/** A new page with a Default Language widget configured with `rows`, published. Returns the host name and the published URL. */
async function publishLangPage(page: Page, shot: string, rows: Row[], reopen?: () => Promise<void>): Promise<{ host: string; url: string }> {
  const host = await createPage(page, `Lang Host ${shot.replace(/^lang-/, '')} ${run} ${Date.now().toString().slice(-4)}`);
  await placeWidget(page, 'percDefaultLang');
  await saveLayout(page);
  const { frame, save } = await openWidgetContentEditor(page, 'percDefaultLang');
  await frame.locator('#perc-content-edit-configurationName').fill(`QA Lang ${shot} ${run}`);
  for (const row of rows) {
    await frame.locator('.perc-table-add').click();
    const r = frame.locator('tr.perc-defaultlang-row:not(.hide)').last();
    await r.locator('.perc-lang-protocol').selectOption(row.protocol);
    await r.locator('.perc-site-list').selectOption({ label: TEST_SITE });
    await r.locator('.perc-lang-list').selectOption({ label: row.language });
    if (row.country) await r.locator('.perc-country-list').selectOption({ label: row.country });
    if (row.isDefault) await r.locator('input[name="default-lang"]').check();
  }
  await page.screenshot({ path: `screenshots/${shot}-content-editor.png` });
  await save.click();
  await expect(save).toBeHidden({ timeout: 60_000 });
  if (reopen) await reopen();
  await openContentTab(page);
  await publishOpenItem(page);
  return { host, url: `${DTS_URL}/${TEST_FOLDER}/${host}.html` };
}

const alternates = async (page: Page) =>
  page.locator('head link[rel="alternate"][hreflang]').evaluateAll(els => els.map(e => ({ lang: e.getAttribute('hreflang')!, href: e.getAttribute('href')! })));

test.describe('Default Language', () => {
  test.setTimeout(240_000);

  test('Default Language: a row with language and region writes hreflang "language-REGION", a row with the language only writes "language", with the protocol of the row', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const { host, url } = await publishLangPage(page, 'lang-rows', [
      { protocol: 'https', language: 'English', country: 'United States' },
      { protocol: 'http', language: 'French', country: 'France' },
      { protocol: 'https', language: 'German' },
    ]);
    await page.goto(url);
    const path = `${TEST_SITE}/${TEST_FOLDER}/${host}.html`.toLowerCase();
    const found = await alternates(page);
    expect(found.find(a => a.lang === 'en-US')?.href, 'English, United States, https').toBe(`https://${path}`);
    expect(found.find(a => a.lang === 'fr-FR')?.href, 'French, France, http').toBe(`http://${path}`);
    expect(found.find(a => a.lang === 'de')?.href, 'German without region, https').toBe(`https://${path}`);
    expect(found.filter(a => a.lang === 'x-default'), 'without a default row there is no x-default').toHaveLength(0);
    await page.screenshot({ path: 'screenshots/lang-published-page-rows.png' });
  });

  test('Default Language: the row marked "Default" also writes the x-default alternate', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const { host, url } = await publishLangPage(page, 'lang-default', [
      { protocol: 'https', language: 'English', country: 'United Kingdom' },
      { protocol: 'https', language: 'Spanish', country: 'Spain', isDefault: true },
    ]);
    await page.goto(url);
    const path = `${TEST_SITE}/${TEST_FOLDER}/${host}.html`.toLowerCase();
    const found = await alternates(page);
    expect(found.find(a => a.lang === 'en-GB')?.href, 'English, United Kingdom').toBe(`https://${path}`);
    expect(found.find(a => a.lang === 'es-ES')?.href, 'Spanish, Spain').toBe(`https://${path}`);
    expect(found.filter(a => a.lang === 'x-default').map(a => a.href), 'x-default points at the default row').toEqual([`https://${path}`]);
    await page.screenshot({ path: 'screenshots/lang-published-page-x-default.png' });
  });

  test('Default Language: a saved configuration is shown again in the editor, a row can be removed, and a removed row writes no alternate', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const { host, url } = await publishLangPage(page, 'lang-edit-rows', [
      { protocol: 'https', language: 'English', country: 'United States' },
      { protocol: 'https', language: 'French', country: 'France' },
      { protocol: 'https', language: 'German' },
    ], async () => {
      // reopen the editor of the widget before the page is published: the three rows are there, in the order they were saved
      const { frame, save } = await openWidgetContentEditor(page, 'percDefaultLang');
      const rowsOf = () => frame.locator('tr.perc-defaultlang-row:not(.hide)');
      await expect(rowsOf(), 'the saved rows are shown again').toHaveCount(3);
      const langs = async () => rowsOf().evaluateAll(els => els.map(e => (e.querySelector('.perc-lang-list') as HTMLSelectElement).value));
      expect(await langs(), 'in the order they were saved').toEqual(['en', 'fr', 'de']);
      await rowsOf().nth(1).locator('.perc-table-remove').click();   // the French row
      expect(await langs(), 'the second row was removed').toEqual(['en', 'de']);
      await page.screenshot({ path: 'screenshots/lang-editor-row-removed.png' });
      await save.click();
      await expect(save).toBeHidden({ timeout: 60_000 });
    });
    await page.goto(url);
    const path = `${TEST_SITE}/${TEST_FOLDER}/${host}.html`.toLowerCase();
    const found = await alternates(page);
    expect(found.map(a => a.lang).sort(), 'the removed row writes no alternate').toEqual(['de', 'en-US']);
    expect(found.find(a => a.lang === 'de')?.href, 'the other rows are still written').toBe(`https://${path}`);
  });

  test('Default Language: the editor refuses a configuration without a name and a row', async ({ page }) => {
    await loginAsAdmin(page);
    await createPage(page, `Lang Host validation ${run} ${Date.now().toString().slice(-4)}`);
    await placeWidget(page, 'percDefaultLang');
    await saveLayout(page);
    const { frame, save } = await openWidgetContentEditor(page, 'percDefaultLang');
    await save.click();
    await expect(save, 'nothing is saved without a name: the editor stays open').toBeVisible({ timeout: 10_000 });
    await page.screenshot({ path: 'screenshots/lang-editor-validation.png' });
  });
});
