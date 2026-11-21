import { Page, expect } from '@playwright/test';
import * as fs from 'fs';
import { CMS_LOG, TEST_SITE, TEST_FOLDER, TEST_ASSET_FOLDER, TEST_TEMPLATE } from './qa-env';

// eslint-disable-next-line @typescript-eslint/no-var-requires
const auth = require('./auth');

/** Logs in as Admin through the module's shared auth helper (credentials are discovered from the install). */
export async function loginAsAdmin(page: Page) {
  await auth.loginAsAdmin(page);
}

/**
 * Makes sure the site and folder that the page/widget scenarios work in exist (QA_SITE / QA_FOLDER), creating them
 * through the UI on a fresh install: the site with its own page template (a base layout with left/content/right
 * regions) and then the folder. Does nothing when they already exist, so it is safe to call before every run.
 */
export async function ensureTestSite(page: Page) {
  const path = page.locator('#mcol-path-summary');
  const site = page.getByText(TEST_SITE, { exact: true }).first();
  await page.goto('/cm/app/?view=dash');
  await expect(page.getByText('Welcome, Admin')).toBeVisible({ timeout: 90_000 });
  await page.getByText('Sites', { exact: true }).first().click();
  await expect(path).toHaveValue('/Sites', { timeout: 30_000 });

  if (!(await site.waitFor({ timeout: 5_000 }).then(() => true, () => false))) {
    await mouseClick(page, '#perc-finder-new-site');
    const dlg = page.locator('.ui-dialog', { hasText: 'Create Site' }).last();
    await dlg.waitFor();
    // the first visible text box on the page is the finder's path box, so always scope to the dialog
    await dlg.locator('input[type=text]:visible').first().pressSequentially(TEST_SITE);
    await dlg.getByText('Percussion Templates').click();
    await dlg.getByRole('button', { name: 'Next' }).click();
    await dlg.getByText('Template name:').waitFor();
    await dlg.locator('input[type=text]:visible').first().pressSequentially(TEST_TEMPLATE);
    await dlg.getByText('rightLeftSidebar', { exact: true }).click();   // layout with left sidebar, content and right sidebar regions
    await dlg.getByRole('button', { name: 'Finish' }).click();
    await expect(path).toHaveValue(`/Sites/${TEST_SITE}`, { timeout: 90_000 });
  }

  await site.click();
  await expect(path).toHaveValue(`/Sites/${TEST_SITE}`, { timeout: 30_000 });
  await ensureFolderHere(page, TEST_FOLDER);

  // the folder that the asset scenarios upload their files into
  await page.getByText('Assets', { exact: true }).first().click();
  await expect(path).toHaveValue('/Assets', { timeout: 30_000 });
  await ensureFolderHere(page, TEST_ASSET_FOLDER);
}

/** In the finder folder that is currently selected: creates the folder `name` unless it is already listed. */
async function ensureFolderHere(page: Page, name: string) {
  const folder = page.getByText(name, { exact: true }).first();
  if (await folder.waitFor({ timeout: 5_000 }).then(() => true, () => false)) return;
  await mouseClick(page, '#perc-finder-new-folder');
  // the server creates "New-Folder" and the finder then shows its name in an edit box: wait for that box, then rename
  const editBox = await page.waitForFunction(() =>
    Array.from(document.querySelectorAll<HTMLInputElement>('input[type=text]'))
      .find(i => i.id !== 'mcol-path-summary' && /^New-Folder/.test(i.value)) ?? null, undefined, { timeout: 30_000 });
  const input = editBox.asElement()!;
  await input.fill(name);
  await input.press('Enter');
  await expect(folder).toBeVisible({ timeout: 30_000 });
}

/**
 * Uploads a file as a new File asset into /Assets/<folder> (New Asset > File) and returns the name the CMS gave the asset
 * (named after the file; a number is added when the name is taken). The new asset is a never-published Draft and is left open in the editor.
 */
export async function uploadFileAsset(page: Page, folder: string, fixturePath: string): Promise<string> {
  const path = page.locator('#mcol-path-summary');
  await page.goto('/cm/app/?view=dash');
  await expect(page.getByText('Welcome, Admin')).toBeVisible({ timeout: 90_000 });
  await page.getByText('Assets', { exact: true }).first().click();
  await expect(path).toHaveValue('/Assets', { timeout: 30_000 });
  await page.getByText(folder, { exact: true }).first().click();
  await expect(path).toHaveValue(`/Assets/${folder}`, { timeout: 30_000 });
  await page.locator('#mcol-new-asset.ui-enabled').click();
  await page.locator('text=/^File$/ >> visible=true').first().click({ timeout: 60_000 });
  await page.getByRole('button', { name: 'Next' }).click();
  const frame = page.frameLocator('iframe[src*="psx_cepercFileAsset"]');
  await frame.locator('input[name="linktext"], input[type="text"]').first().fill(`QA-${Date.now()}`);
  await frame.locator('input[type="file"]').setInputFiles(fixturePath);
  await page.getByRole('button', { name: 'Save' }).click();
  await page.waitForURL(/view=editAsset.*name=/, { timeout: 60_000 });
  return new URL(page.url()).searchParams.get('name')!;
}

/** Selects a page in the finder (Sites > site > page), waiting for each level to load. Returns after the page is selected. */
export async function selectPage(page: Page, site: string, name: string, open = false) {
  await page.goto('/cm/app/?view=dash');
  await expect(page.getByText('Welcome, Admin')).toBeVisible({ timeout: 90_000 });
  const path = page.locator('#mcol-path-summary');
  await page.getByText('Sites', { exact: true }).first().click();
  await expect(path).toHaveValue('/Sites', { timeout: 30_000 });
  await page.getByText(site, { exact: true }).first().click();
  await expect(path).toHaveValue(`/Sites/${site}`, { timeout: 30_000 });
  const item = page.getByText(name, { exact: true }).first();
  await expect(item).toBeVisible({ timeout: 30_000 });
  if (open) await item.dblclick(); else await item.click();
  await expect(path).toHaveValue(`/Sites/${site}/${name}`, { timeout: 30_000 });
}


/** With an item open in the editor, runs Publishing > Publish and waits for the publish job to finish (server.log). */
export async function publishOpenItem(page: Page) {
  const fsx = await import('fs');
  const count = () => fsx.readFileSync(CMS_LOG).toString().split('Publishing Finished').length;
  const before = count();
  await page.getByText('Publishing', { exact: true }).first().click({ timeout: 60_000 });
  await page.locator('li:visible', { hasText: /^Publish$/ }).last().click();
  await expect.poll(count, { timeout: 120_000 }).toBeGreaterThan(before);
}

/** Reads the "Last Modified" and "Last Published" lines of the finder info panel for the selected item. */
export async function readItemDates(page: Page) {
  const txt = await page.locator('body').innerText();
  return {
    modified: txt.match(/Last Modified:\s*\S+\s*\n([^\n]+)/)?.[1]?.trim(),
    published: txt.match(/Last Published:\s*\n([^\n]+)/)?.[1]?.trim(),
  };
}

/** Real mouse click at the centre of an element (finder toolbar buttons ignore synthetic/forced clicks). */
export async function mouseClick(page: Page, selector: string) {
  const b = await page.locator(selector).boundingBox();
  if (!b) throw new Error(`no box for ${selector}`);
  await page.mouse.click(b.x + b.width / 2, b.y + b.height / 2);
}

/** Opens the editor Layout tab with a real mouse click, retrying until the tab reports active (tabs initialise late). */
export async function openLayoutTab(page: Page) {
  await expect(async () => {
    await mouseClick(page, '#perc-tab-layout');
    await expect(page.locator('li:has(> #perc-tab-layout)')).toHaveClass(/ui-tabs-active|ui-state-active/, { timeout: 2_000 });
  }).toPass({ timeout: 60_000 });
}

/**
 * Layout tab: opens the widget library and drags a tile (e.g. 'percArchiveList') into a region of the
 * page frame (default the centre 'content' region) using a stepwise real-mouse drag (jQuery UI draggable).
 * Returns the widget's drop position so callers can screenshot/assert around it.
 */
export async function addWidget(page: Page, widgetKey: string, region: 'leftsidebar' | 'content' | 'rightsidebar' = 'content') {
  await openLayoutTab(page);
  const lib = page.locator('#perc-widget-library');
  if (!(await lib.isVisible())) await page.getByText('Add Widget').first().click();
  const tile = page.locator(`#widget-${widgetKey}-0`);
  await tile.waitFor({ state: 'visible' });
  const frame = page.frameLocator('iframe#frame');
  const regionBox = await frame.locator(`#${region}`).boundingBox();
  const frameBox = await page.locator('iframe#frame').boundingBox();
  if (!regionBox || !frameBox) throw new Error('region/frame box missing');
  // boundingBox of an element inside a frame is already in page coordinates.
  const target = { x: regionBox.x + regionBox.width / 2, y: regionBox.y + Math.min(60, regionBox.height / 2) };
  const t = (await tile.boundingBox())!;
  await page.mouse.move(t.x + t.width / 2, t.y + t.height / 2);
  await page.mouse.down();
  await page.mouse.move(t.x + 40, t.y + 60, { steps: 5 });
  await page.mouse.move(target.x, target.y, { steps: 25 });
  await page.mouse.up();
  return target;
}

/** Creates a page from the qa-base template in the configured test site/folder (QA_SITE/QA_FOLDER) and leaves it open in the editor (edit mode). Returns the file name. */
export async function createPage(page: Page, title: string): Promise<string> {
  await page.goto('/cm/app/?view=dash');
  await expect(page.getByText('Welcome, Admin')).toBeVisible({ timeout: 90_000 });
  const path = page.locator('#mcol-path-summary');
  await page.getByText('Sites', { exact: true }).first().click();
  await expect(path).toHaveValue('/Sites');
  await page.getByText(TEST_SITE, { exact: true }).first().click();
  await expect(path).toHaveValue(`/Sites/${TEST_SITE}`);
  await page.getByText(TEST_FOLDER, { exact: true }).first().click();
  await expect(path).toHaveValue(`/Sites/${TEST_SITE}/${TEST_FOLDER}`);
  await page.locator('#mcol-new-page.ui-enabled').click();
  const dlg = page.locator('.ui-dialog:visible').filter({ hasText: 'New Page' });
  await expect(dlg).toBeVisible({ timeout: 30_000 });
  await dlg.locator('#perc-page-linktext').pressSequentially(title, { delay: 30 });
  const name = await dlg.locator('#perc-page-name').inputValue();
  expect(name, 'file name auto-filled from link text').not.toBe('');
  await dlg.getByRole('button', { name: 'Save' }).click();
  await expect(page).toHaveURL(/view=editor.*mode=edit/, { timeout: 60_000 });
  return name;
}
