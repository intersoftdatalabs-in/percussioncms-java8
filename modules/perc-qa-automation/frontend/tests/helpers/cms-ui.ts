import { Page, expect } from '@playwright/test';
import * as fs from 'fs';
import { CMS_LOG, DTS_WEB_ROOT, TEST_SITE, TEST_FOLDER, TEST_ASSET_FOLDER, TEST_TEMPLATE } from './qa-env';

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

/** Makes sure `/Sites/<TEST_SITE>/<name>` exists, creating it through the finder when missing. Safe to call again. */
export async function ensureSiteFolder(page: Page, name: string) {
  const path = page.locator('#mcol-path-summary');
  await page.goto('/cm/app/?view=dash');
  await expect(page.getByText('Welcome, Admin')).toBeVisible({ timeout: 90_000 });
  await page.getByText('Sites', { exact: true }).first().click();
  await expect(path).toHaveValue('/Sites', { timeout: 30_000 });
  await page.getByText(TEST_SITE, { exact: true }).first().click();
  await expect(path).toHaveValue(`/Sites/${TEST_SITE}`, { timeout: 30_000 });
  await ensureFolderHere(page, name);
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
export async function uploadFileAsset(page: Page, folder: string, fixturePath: string, linkText: string = `QA-${Date.now()}`): Promise<string> {
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
  await frame.locator('input[name="linktext"], input[type="text"]').first().fill(linkText);
  await frame.locator('input[type="file"]').setInputFiles(fixturePath);
  await page.getByRole('button', { name: 'Save' }).click();
  await page.waitForURL(/view=editAsset.*name=/, { timeout: 60_000 });
  return new URL(page.url()).searchParams.get('name')!;
}

/**
 * Publish > QA site > Server Properties: sets the DTS server ('NONE' or the DTS admin URL label) and where the files are written:
 * `webRoot: 'dts'` writes into the web root the DTS serves (so the published page can be opened over HTTP and its delivery
 * services work); `'default'` restores the "Server Default" folder (<site>apps/ROOT) that the local-publish scenarios inspect.
 */
export async function configurePublishing(page: Page, dts: string, webRoot: 'default' | 'dts') {
  await page.goto('/cm/app/?view=publish&pathType=unknown');
  await page.getByText(TEST_SITE, { exact: true }).first().click();
  await page.locator('.perc-edit-server-properties').click({ timeout: 30_000 });
  const field = page.locator('select[name="publishServer"]');
  await expect(field).toBeVisible({ timeout: 30_000 });
  await field.selectOption({ label: dts });
  if (webRoot === 'dts') {
    await page.getByText('Exact Path', { exact: true }).click();
    await page.locator('#ownServer').fill(DTS_WEB_ROOT);
  } else {
    await page.getByText('Server Default', { exact: true }).click();
  }
  await page.getByRole('button', { name: /save/i }).first().click();
  await expect(field).toBeHidden({ timeout: 30_000 });
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
  // the library shows one type at a time (Percussion, Custom, Deprecated (Pending Removal)): look in the others when the tile is not in this one
  if ((await tile.count()) === 0) {
    const typeSelect = lib.locator('select').first();
    for (const label of ['Custom', 'Deprecated (Pending Removal)', 'Percussion']) {
      await typeSelect.selectOption({ label });
      if ((await tile.count()) > 0) break;
    }
  }
  await tile.waitFor({ state: 'visible' });
  await tile.scrollIntoViewIfNeeded();   // tiles further along the library are clipped until scrolled to
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

/**
 * Pages published through "Publish" on a single item do not deliver the shared scripts they reference
 * (/web_resources/cm/jslib/jquery.js, perc_common_ui.js, ...). A Full publish of the site does, so run one when the DTS web root
 * does not have them yet. Needed by every widget whose list is built in the browser from DTS services.
 */
export async function ensureDeliveryResources(page: Page) {
  const marker = `${DTS_WEB_ROOT}/web_resources/cm/jslib/jquery.js`;
  if (fs.existsSync(marker)) return;
  const count = () => fs.readFileSync(CMS_LOG).toString().split('Publishing Finished').length;
  const before = count();
  await page.goto('/cm/app/?view=publish&pathType=unknown');
  await page.getByText(TEST_SITE, { exact: true }).first().click();
  const full = page.locator('a.perc-publish-button[data-perc-publish-type="full"]').first();
  if (!(await full.isVisible())) await page.getByRole('button', { name: 'Toggle dropdown' }).first().click({ timeout: 15_000 });
  await full.click();
  await expect.poll(count, { timeout: 240_000 }).toBeGreaterThan(before);
  await expect.poll(() => fs.existsSync(marker), { timeout: 120_000 }).toBeTruthy();
}

/** Site Navigation view (view=arch) of the test site. */
export async function openNavigationView(page: Page) {
  await page.goto(`/cm/app/?view=arch&site=${encodeURIComponent(TEST_SITE)}&path=${encodeURIComponent('/Sites/' + TEST_SITE)}`);
  await expect(page.locator('.perc-site-map-sectiontitle').first()).toBeVisible({ timeout: 60_000 });
}

/**
 * Navigation view: adds a section (type "Section & landing page") on `level` (2 = directly below Home, 3 = below the section
 * that is expanded). `folderName` becomes the section's folder. The "add section" button of a level is the first one in its row.
 */
async function addSection(page: Page, level: 2 | 3, title: string, folderName: string) {
  const addButton = level === 2 ? { x: 52, y: 617 } : { x: 52, y: 763 };
  const dlg = page.locator('.ui-dialog:visible', { hasText: 'New Section' });
  await expect(async () => {            // the map binds the button's click handler a moment after drawing it
    if (!(await dlg.isVisible())) await page.mouse.click(addButton.x, addButton.y);
    await expect(dlg).toBeVisible({ timeout: 3_000 });
  }).toPass({ timeout: 60_000 });
  const inputs = dlg.locator('input[type=text]:visible');
  await inputs.nth(0).fill(title);
  await inputs.nth(1).fill(folderName);
  await dlg.getByRole('button', { name: 'Save' }).click();
  await expect(dlg).toBeHidden({ timeout: 60_000 });
  await expect(page.locator('.perc-site-map-sectiontitle', { hasText: title })).toBeVisible({ timeout: 60_000 });
}

/** Makes sure a level-2 section with this title exists (creates it when missing) and expands it so its children (level 3) show. */
export async function ensureLevel2Section(page: Page, title: string, folderName: string) {
  await openNavigationView(page);
  const node = page.locator('.perc-site-map-sectiontitle', { hasText: title });
  if (!(await node.first().isVisible().catch(() => false))) await addSection(page, 2, title, folderName);
  // the "children" arrow on the section's box; the map binds its click handlers a moment after it is drawn, so retry
  await expect(async () => {
    const box = (await node.first().boundingBox())!;
    await page.mouse.click(box.x + 47, box.y + 79);
    await expect(page.getByText('Level 3')).toBeVisible({ timeout: 3_000 });
  }).toPass({ timeout: 60_000 });
}

/** Adds a level-3 section below the expanded level-2 section (call ensureLevel2Section first). */
export async function addLevel3Section(page: Page, title: string, folderName: string) {
  await addSection(page, 3, title, folderName);
}

/** Switches an open page in the editor to edit mode, unless it already opened directly in edit mode (e.g. Quick Edit). */
async function enterEditMode(page: Page) {
  await expect(page).toHaveURL(/view=editor/, { timeout: 60_000 });
  // double click opens a checked-in page read-only ("Viewing Page"); a page already in Quick Edit opens straight in edit mode with no Edit button
  const editButton = page.getByRole('button', { name: 'Edit', exact: true });
  await expect(async () => {
    if (/mode=edit/.test(page.url())) return;
    if (await editButton.isVisible()) {
      await editButton.click({ timeout: 5_000 });
      return;
    }
    expect(page.url()).toMatch(/mode=edit/);
  }).toPass({ timeout: 60_000 });
  await expect(page).toHaveURL(/mode=edit/, { timeout: 60_000 });
}

/**
 * Opens a page in the editor in EDIT mode by clicking through the finder: Sites > site > folders... > page. The page is picked in the
 * deepest finder column (the same file name, e.g. index.html, also exists in the columns to the left) and its full path is checked.
 */
export async function openPageByPath(page: Page, segments: string[], pageName: string) {
  await page.goto('/cm/app/?view=dash');
  await expect(page.getByText('Welcome, Admin')).toBeVisible({ timeout: 90_000 });
  const path = page.locator('#mcol-path-summary');
  await page.getByText('Sites', { exact: true }).first().click();
  let current = '/Sites';
  await expect(path).toHaveValue(current, { timeout: 30_000 });
  for (const s of [TEST_SITE, ...segments]) {
    await page.getByText(s, { exact: true }).last().click();
    current += `/${s}`;
    await expect(path).toHaveValue(current, { timeout: 30_000 });
  }
  const item = page.getByText(pageName, { exact: true }).last();
  await expect(item).toBeVisible({ timeout: 30_000 });
  await item.dblclick();
  await expect(path).toHaveValue(`${current}/${pageName}`, { timeout: 30_000 });
  await enterEditMode(page);
}

/**
 * Uploads an image as a new Image asset into /Assets/<folder> (New Asset > Image) with the given title and alt text and returns the name
 * the CMS gave the asset. The new asset is a never-published Draft and is left open in the asset editor.
 */
export async function uploadImageAsset(page: Page, folder: string, fixturePath: string, title: string, alt: string, linkText?: string): Promise<string> {
  const path = page.locator('#mcol-path-summary');
  await page.goto('/cm/app/?view=dash');
  await expect(page.getByText('Welcome, Admin')).toBeVisible({ timeout: 90_000 });
  await page.getByText('Assets', { exact: true }).first().click();
  await expect(path).toHaveValue('/Assets', { timeout: 30_000 });
  await page.getByText(folder, { exact: true }).first().click();
  await expect(path).toHaveValue(`/Assets/${folder}`, { timeout: 30_000 });
  await page.locator('#mcol-new-asset.ui-enabled').click();
  await page.locator('text=/^Image$/ >> visible=true').first().click({ timeout: 60_000 });
  await page.getByRole('button', { name: 'Next' }).click();
  const frame = page.frameLocator('iframe[src*="psx_cepercImageAsset"]');
  await frame.locator('#perc-content-edit-displaytitle').pressSequentially(title, { delay: 10 });
  await frame.locator('#perc-content-edit-alttext').pressSequentially(alt, { delay: 10 });
  await frame.locator('#perc-select-image').setInputFiles(fixturePath);
  await expect(frame.locator('#asset_main_width'), 'size of the chosen image is read').not.toHaveValue('', { timeout: 30_000 });
  if (linkText) {
    // "Link text" is in the Meta-data section; the Image Auto-List shows it as the caption
    await frame.getByText('Meta-data').first().click();
    await frame.locator('#perc-content-edit-resource_link_title').pressSequentially(linkText, { delay: 10 });
  }
  await page.getByRole('button', { name: 'Save' }).click();
  await page.waitForURL(/view=editAsset.*name=/, { timeout: 60_000 });
  return new URL(page.url()).searchParams.get('name')!;
}

/**
 * Opens a page that was just published (and so is checked in) in the editor again, in edit mode. `editUrl` is the editor address saved while the page was open
 * (it only selects the page in the finder; the finder lists just the first 100 pages of a folder), `host` is the page file name without .html.
 */
export async function reopenPageInEditor(page: Page, editUrl: string, host: string) {
  await page.goto(editUrl);
  const item = page.getByText(`${host}.html`, { exact: true }).last();
  await expect(item).toBeVisible({ timeout: 60_000 });
  await item.dblclick();
  await enterEditMode(page);
}
