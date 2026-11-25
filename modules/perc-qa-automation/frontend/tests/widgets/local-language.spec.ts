import { test, expect, Page } from '@playwright/test';
import { loginAsAdmin, createPage, publishOpenItem, configurePublishing } from '../helpers/cms-ui';
import { placeWidget, saveLayout, openContentTab, openWidgetContentEditor } from '../helpers/widgets-ui';
import { choosePageFromTree } from '../helpers/directory-ui';
import { DTS_URL, DTS_ADMIN_URL, TEST_FOLDER, TEST_SITE } from '../helpers/qa-env';

/**
 * Local Language: for international sites. Like Default Language, but each row of the table in the widget's editor names the alternate page itself (chosen with
 * Browse > "Select a page"): the widget writes a <link rel="alternate" hreflang="..."> for each row into the head of the published page, and x-default for the
 * default row. The proof is the PUBLISHED page served by the DTS. Three pages are created and published first, to be the alternates.
 */
process.env.QA_LOCLANG_RUN ??= Date.now().toString().slice(-7);   // shared with the workers Playwright restarts after a failed scenario
const run = process.env.QA_LOCLANG_RUN;

type Row = { protocol: 'http' | 'https'; target: number; language: string; country?: string; isDefault?: boolean };
const targets: string[] = ['', '', ''];   // file names (without .html) of the three alternate pages, filled in by the first scenario
const targetTitle = (n: number) => `AA Lang Target ${n} ${run}`;
const hrefOf = (protocol: string, file: string) => `${protocol}://${TEST_SITE}/${TEST_FOLDER}/${file}.html`.toLowerCase();

/** A new page with a Local Language widget configured with `rows`, published. Optionally runs `reopen` (the editor opened again) before the publication. */
async function publishLangPage(page: Page, shot: string, rows: Row[], reopen?: () => Promise<void>): Promise<string> {
  const host = await createPage(page, `Local Lang Host ${shot.replace(/^locallang-/, '')} ${run} ${Date.now().toString().slice(-4)}`);
  await placeWidget(page, 'percLocalLang');
  await saveLayout(page);
  const { frame, save } = await openWidgetContentEditor(page, 'percLocalLang');
  await frame.locator('#perc-content-edit-configurationName').fill(`QA Local Lang ${shot} ${run}`);
  for (const row of rows) {
    await frame.locator('.perc-table-add').click();
    const r = frame.locator('tr.perc-local-lang-row:not(.hide)').last();
    await r.locator('.perc-lang-protocol').selectOption(row.protocol);
    await choosePageFromTree(page, r.locator('.perc-page-field-select-button'), targets[row.target]);
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
  return `${DTS_URL}/${TEST_FOLDER}/${host}.html`;
}

const alternates = async (page: Page) =>
  page.locator('head link[rel="alternate"][hreflang]').evaluateAll(els => els.map(e => ({ lang: e.getAttribute('hreflang')!, href: e.getAttribute('href')! })));

test.describe.serial('Local Language', () => {
  test.setTimeout(300_000);

  test('Local Language: each row writes hreflang "language-REGION" (or "language") pointing at the page chosen for it, with the protocol of the row', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    for (let n = 0; n < 3; n++) {
      targets[n] = await createPage(page, targetTitle(n + 1));
      await openContentTab(page);
      await publishOpenItem(page);
    }
    const url = await publishLangPage(page, 'locallang-rows', [
      { protocol: 'https', target: 0, language: 'English', country: 'United States' },
      { protocol: 'http', target: 1, language: 'French', country: 'France' },
      { protocol: 'https', target: 2, language: 'German' },
    ]);
    await page.goto(url);
    const found = await alternates(page);
    expect(found.find(a => a.lang === 'en-US')?.href, 'English, United States, https, first page').toBe(hrefOf('https', targets[0]));
    expect(found.find(a => a.lang === 'fr-FR')?.href, 'French, France, http, second page').toBe(hrefOf('http', targets[1]));
    expect(found.find(a => a.lang === 'de')?.href, 'German without region, https, third page').toBe(hrefOf('https', targets[2]));
    expect(found.filter(a => a.lang === 'x-default'), 'without a default row there is no x-default').toHaveLength(0);
    await page.screenshot({ path: 'screenshots/locallang-published-page-rows.png' });
  });

  test('Local Language: the row marked "Default" also writes the x-default alternate', async ({ page }) => {
    expect(targets[0], 'the pages of the first scenario').toBeTruthy();
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishLangPage(page, 'locallang-default', [
      { protocol: 'https', target: 0, language: 'English', country: 'United Kingdom' },
      { protocol: 'https', target: 1, language: 'Spanish', country: 'Spain', isDefault: true },
    ]);
    await page.goto(url);
    const found = await alternates(page);
    expect(found.find(a => a.lang === 'en-GB')?.href, 'English, United Kingdom').toBe(hrefOf('https', targets[0]));
    expect(found.find(a => a.lang === 'es-ES')?.href, 'Spanish, Spain').toBe(hrefOf('https', targets[1]));
    expect(found.filter(a => a.lang === 'x-default').map(a => a.href), 'x-default points at the page of the default row').toEqual([hrefOf('https', targets[1])]);
    await page.screenshot({ path: 'screenshots/locallang-published-page-x-default.png' });
  });

  test('Local Language: a saved configuration is shown again in the editor, a row can be removed, and a removed row writes no alternate', async ({ page }) => {
    expect(targets[0], 'the pages of the first scenario').toBeTruthy();
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishLangPage(page, 'locallang-edit-rows', [
      { protocol: 'https', target: 0, language: 'English', country: 'United States' },
      { protocol: 'https', target: 1, language: 'French', country: 'France' },
      { protocol: 'https', target: 2, language: 'German' },
    ], async () => {
      // reopen the editor of the widget before the page is published: the three rows are there with the pages that were chosen
      const { frame, save } = await openWidgetContentEditor(page, 'percLocalLang');
      const rowsOf = () => frame.locator('tr.perc-local-lang-row:not(.hide)');
      await expect(rowsOf(), 'the saved rows are shown again').toHaveCount(3);
      const langs = async () => rowsOf().evaluateAll(els => els.map(e => (e.querySelector('.perc-lang-list') as HTMLSelectElement).value));
      expect(await langs(), 'in the order they were saved').toEqual(['en', 'fr', 'de']);
      await expect(rowsOf().nth(1).locator('.perc-page-select input[type="text"]'), 'the page chosen for the second row').toHaveAttribute('title', new RegExp(`${targets[1]}\\.html$`));
      await rowsOf().nth(1).locator('.perc-table-remove').click();   // the French row
      expect(await langs(), 'the second row was removed').toEqual(['en', 'de']);
      await page.screenshot({ path: 'screenshots/locallang-editor-row-removed.png' });
      await save.click();
      await expect(save).toBeHidden({ timeout: 60_000 });
    });
    await page.goto(url);
    const found = await alternates(page);
    expect(found.map(a => a.lang).sort(), 'the removed row writes no alternate').toEqual(['de', 'en-US']);
    expect(found.find(a => a.lang === 'de')?.href, 'the other rows are still written').toBe(hrefOf('https', targets[2]));
  });

  test('Local Language: the editor refuses a configuration without a name', async ({ page }) => {
    await loginAsAdmin(page);
    await createPage(page, `Local Lang Host validation ${run} ${Date.now().toString().slice(-4)}`);
    await placeWidget(page, 'percLocalLang');
    await saveLayout(page);
    const { save } = await openWidgetContentEditor(page, 'percLocalLang');
    await save.click();
    await expect(save, 'nothing is saved without a name: the editor stays open').toBeVisible({ timeout: 10_000 });
    await page.screenshot({ path: 'screenshots/locallang-editor-validation.png' });
  });
});
