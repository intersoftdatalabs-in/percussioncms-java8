import { test, expect, Page } from '@playwright/test';
import * as path from 'path';
import { loginAsAdmin, createPage, publishOpenItem, configurePublishing, uploadFileAsset } from '../helpers/cms-ui';
import { placeWidget, saveLayout, openContentTab, openWidgetContentEditor, openWidgetConfigure, confirmWidgetConfigure } from '../helpers/widgets-ui';
import { DTS_URL, DTS_ADMIN_URL, TEST_FOLDER, TEST_ASSET_FOLDER } from '../helpers/qa-env';

/**
 * File Auto-List: lists the File assets that match criteria set in the widget's content editor (title contains, asset folder).
 * The list is built by the CMS when the page is published, so the proof is the PUBLISHED page served by the DTS. Four PDF assets are
 * uploaded first: three whose title contains the run token and one that does not.
 */
const PDF = path.join(__dirname, '..', 'fixtures', 'test.pdf');
const run = Date.now().toString().slice(-7);
const token = `QA FAL ${run}`;
const titles = ['Alpha', 'Bravo', 'Charlie'].map(n => `${token} ${n}`);
const unrelated = `Unrelated asset ${run}`;

/** Puts a File Auto-List on a new page, optionally configures it, sets the criteria (title contains the token, the QA asset folder) and publishes. */
async function publishListPage(page: Page, shot: string, configure?: (form: ReturnType<Page['locator']>) => Promise<void>): Promise<string> {
  const host = await createPage(page, `File List Host ${shot.slice(-6)} ${run}`);
  await placeWidget(page, 'percFileAutoList');
  if (configure) {
    const form = await openWidgetConfigure(page, 'percFileAutoList');
    await configure(form);
    await page.screenshot({ path: `screenshots/${shot}-configure-dialog.png` });
    await confirmWidgetConfigure(page);
  }
  await saveLayout(page);

  const { frame, save } = await openWidgetContentEditor(page, 'percFileAutoList');
  await frame.locator('#perc-content-edit-displaytitle').pressSequentially(`Files ${run}`, { delay: 10 });
  await frame.locator('#display_title_contains').pressSequentially(token, { delay: 10 });
  await frame.locator('.fancytree-expander').first().click({ timeout: 15_000 });
  await frame.locator('.fancytree-title', { hasText: new RegExp(`^${TEST_ASSET_FOLDER}$`) }).click({ timeout: 15_000 });
  await page.screenshot({ path: `screenshots/${shot}-content-editor-criteria.png` });
  await save.click();
  await expect(save).toBeHidden({ timeout: 60_000 });
  await openContentTab(page);
  await publishOpenItem(page);
  return `${DTS_URL}/${TEST_FOLDER}/${host}.html`;
}

test.describe.serial('File Auto-List', () => {
  test.setTimeout(480_000);

  test('File Auto-List: published page lists the file assets whose title contains the text, from the chosen asset folder, and not the others', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    // auto-lists only show assets that have been published, so each asset is published right after the upload
    for (const t of [...titles, unrelated]) {
      await uploadFileAsset(page, TEST_ASSET_FOLDER, PDF, t);
      await publishOpenItem(page);
    }
    const url = await publishListPage(page, 'file-auto-list-title-filter');

    await page.goto(url);
    const list = page.locator('.perc-file-auto-list, .perc-list-vertical').first();
    for (const t of titles) await expect(page.getByRole('link', { name: t }), `"${t}" is listed`).toBeVisible({ timeout: 30_000 });
    await expect(page.getByRole('link', { name: unrelated }), 'asset whose title does not match is not listed').toHaveCount(0);
    await expect(page.getByText(`Files ${run}`).first(), 'list title').toBeVisible();
    await page.screenshot({ path: 'screenshots/file-auto-list-published-page-lists-matching-files.png' });
    expect(await list.count()).toBeGreaterThan(0);
  });

  test('File Auto-List: max list length and the list type from Configure are applied to the published list', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishListPage(page, 'file-auto-list-max-and-ordered', async form => {
      await form.locator('select[name="listType"]').selectOption('ordered', { timeout: 10_000 });
      await form.locator('input[name="maxlength"]').fill('2');
    });

    await page.goto(url);
    const items = page.locator('ol li').filter({ has: page.getByRole('link', { name: new RegExp(token) }) });
    await expect(items, 'only 2 of the 3 matching files are listed, as an ordered list').toHaveCount(2, { timeout: 30_000 });
    await page.screenshot({ path: 'screenshots/file-auto-list-published-page-ordered-list-limited-to-two.png' });
  });
});
