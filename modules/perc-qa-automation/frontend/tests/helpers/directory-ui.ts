import { expect, Page, FrameLocator } from '@playwright/test';
import { mouseClick } from './cms-ui';
import { TEST_SITE, TEST_FOLDER } from './qa-env';

/**
 * Helpers for the Directory widgets (Organization, Department, Person, Directory). Their assets find each other by the folders of the asset library: the
 * Organization of a Department is the Organization asset in the folder of the Department, or the nearest one above it (percAssetFinderControl), and the parent
 * of an Organization is the one above its own folder. A test therefore builds a small folder tree under Assets and creates the assets in it.
 */

/** Opens Assets > each folder of `segments` in turn in the finder (the folders must exist). */
export async function openAssetFolder(page: Page, segments: string[]) {
  const path = page.locator('#mcol-path-summary');
  await page.goto('/cm/app/?view=dash');
  await expect(page.getByText('Welcome, Admin')).toBeVisible({ timeout: 90_000 });
  await page.getByText('Assets', { exact: true }).first().click();
  await expect(path).toHaveValue('/Assets', { timeout: 30_000 });
  let current = '/Assets';
  for (const s of segments) {
    await page.getByText(s, { exact: true }).first().click({ timeout: 30_000 });
    current += `/${s}`;
    await expect(path).toHaveValue(current, { timeout: 30_000 });
  }
}

/** In the finder folder that is currently selected: creates the folder `name` unless it is already listed, and leaves it listed. */
async function ensureFolderHere(page: Page, name: string) {
  const folder = page.getByText(name, { exact: true }).first();
  if (await folder.waitFor({ timeout: 4_000 }).then(() => true, () => false)) return;
  await mouseClick(page, '#perc-finder-new-folder');
  const editBox = await page.waitForFunction(() =>
    Array.from(document.querySelectorAll<HTMLInputElement>('input[type=text]'))
      .find(i => i.id !== 'mcol-path-summary' && /^New-Folder/.test(i.value)) ?? null, undefined, { timeout: 30_000 });
  const input = editBox.asElement()!;
  await input.fill(name);
  await input.press('Enter');
  await expect(folder).toBeVisible({ timeout: 30_000 });
}

/** Makes sure Assets > segments[0] > segments[1] ... exists and leaves its last folder selected in the finder. */
export async function ensureAssetFolder(page: Page, segments: string[]) {
  const path = page.locator('#mcol-path-summary');
  await page.goto('/cm/app/?view=dash');
  await expect(page.getByText('Welcome, Admin')).toBeVisible({ timeout: 90_000 });
  await page.getByText('Assets', { exact: true }).first().click();
  await expect(path).toHaveValue('/Assets', { timeout: 30_000 });
  let current = '/Assets';
  for (const s of segments) {
    await ensureFolderHere(page, s);
    await page.getByText(s, { exact: true }).first().click({ timeout: 30_000 });
    current += `/${s}`;
    await expect(path).toHaveValue(current, { timeout: 30_000 });
  }
}

/**
 * Creates an asset of the type `typeLabel` ("Organization", "Department", "Person") in Assets > `folders` (created when missing), with the text fields
 * `fields` (input name > value) in its editor, and saves it. Returns the name the CMS gave the asset.
 */
export async function createAssetIn(page: Page, folders: string[], typeLabel: string, editorFrame: string, fields: Record<string, string>, inEditor?: (frame: FrameLocator) => Promise<void>): Promise<string> {
  await ensureAssetFolder(page, folders);
  await page.locator('#mcol-new-asset.ui-enabled').click();
  await page.locator(`text=/^${typeLabel}$/ >> visible=true`).first().click({ timeout: 60_000 });
  const frame = page.frameLocator(`iframe[src*="${editorFrame}"]`);
  await expect(async () => {   // the type list needs a moment before Next takes effect
    await page.getByRole('button', { name: 'Next' }).click({ timeout: 3_000 });
    await expect(page.locator(`iframe[src*="${editorFrame}"]`)).toBeVisible({ timeout: 8_000 });
  }).toPass({ timeout: 60_000 });
  const all = { sys_title: Object.values(fields)[0], ...fields };   // "Name" is required: the first field's value unless given
  for (const [name, value] of Object.entries(all)) await frame.locator(`input[name="${name}"]`).fill(value, { timeout: 30_000 });
  if (inEditor) await inEditor(frame);
  await page.getByRole('button', { name: 'Save' }).click();
  await page.waitForURL(/view=editAsset.*name=/, { timeout: 60_000 });
  return new URL(page.url()).searchParams.get('name')!;
}

/**
 * Content tab: drags the shared asset `assetName` from the library (finder: Assets > `folders`) onto the widget `widgetKey` of the open page. The widget then
 * shows that asset. Uses real mouse moves (the finder and the widget use jQuery UI drag and drop).
 */
export async function dragAssetOntoWidget(page: Page, widgetKey: string, folders: string[], assetName: string) {
  const path = page.locator('#mcol-path-summary');
  await page.getByText('Assets', { exact: true }).first().click({ timeout: 30_000 });
  await expect(path).toHaveValue('/Assets', { timeout: 30_000 });
  let current = '/Assets';
  for (const s of folders) {
    await page.getByText(s, { exact: true }).first().click({ timeout: 30_000 });
    current += `/${s}`;
    await expect(path).toHaveValue(current, { timeout: 30_000 });
  }
  const item = page.getByText(assetName, { exact: true }).first();
  await expect(item, 'the asset is listed in its folder').toBeVisible({ timeout: 30_000 });
  const target = page.frameLocator('iframe#frame').locator(`.perc-widget[widgetdefid="${widgetKey}"]`).first();
  const ib = (await item.boundingBox())!;
  const wb = (await target.boundingBox())!;
  await page.mouse.move(ib.x + 10, ib.y + 8);
  await page.mouse.down();
  await page.mouse.move(ib.x + 40, ib.y + 40, { steps: 5 });
  await page.mouse.move(wb.x + wb.width / 2, wb.y + wb.height / 2, { steps: 15 });
  await page.waitForTimeout(500);
  await page.mouse.up();
  await expect(target.getByText(/\S/).first(), 'the widget shows the asset that was dropped on it').toBeVisible({ timeout: 30_000 });
}

/**
 * Takes a shared asset through the workflow to Live (Draft > Review > Pending > Live). The pages the DTS serves are assembled from public assets: an
 * Organization that is only a Draft is not found by the Department, Person and Directory widgets. Opens Assets > `folders`, then for each of the three
 * steps opens the asset (double click > Edit) and presses Submit / Approve / Publish.
 */
export async function takeAssetToLive(page: Page, folders: string[], assetName: string): Promise<void> {
  await openAssetFolder(page, folders);
  const isPublic = async () => {
    await page.getByText(assetName, { exact: true }).first().click({ timeout: 30_000 });
    await page.waitForTimeout(2_000);
    return /Status:\s*(Live|Pending)/.test(await page.locator('body').innerText());
  };
  for (let step = 0; step < 4 && !(await isPublic()); step++) {
    await page.getByText(assetName, { exact: true }).first().dblclick({ timeout: 30_000 });
    await page.getByRole('button', { name: 'Edit' }).click({ timeout: 30_000 });
    const next = page.getByRole('button', { name: /^(Submit|Approve|Publish)$/ }).first();
    await next.waitFor({ timeout: 30_000 });
    await next.click();
    const comments = page.locator('.ui-dialog:visible', { hasText: 'Enter Comments' });
    await comments.getByRole('button').filter({ hasNotText: 'Cancel' }).last().click({ timeout: 10_000 });
    await expect(comments).toBeHidden({ timeout: 30_000 });
    await page.waitForTimeout(3_000);
  }
  expect(await isPublic(), `${assetName} is Pending or Live`).toBe(true);
}

/** An asset editor's Browse > "Select ... page" tree: Sites > site > folder > the page (by file name) > Ok / Save. */
export async function choosePageFromTree(page: Page, browse: import('@playwright/test').Locator, pageName: string) {
  await browse.click({ timeout: 15_000 });
  const dlg = page.locator('.ui-dialog:visible').filter({ hasText: /Select.*page/i });
  await expect(dlg).toBeVisible({ timeout: 15_000 });
  const title = (t: string) => dlg.locator('.fancytree-title', { hasText: new RegExp(`^${t}$`) });
  const open = async (folder: string, child: string) => {
    if (!(await title(child).first().isVisible())) {
      await dlg.locator(`.fancytree-node:has(> .fancytree-title:text-is("${folder}")) > .fancytree-expander`).click({ timeout: 15_000 });
    }
    await expect(title(child), `${child} is listed under ${folder}`).toBeVisible({ timeout: 30_000 });
  };
  const file = `${pageName}\.html`;
  await open('Sites', TEST_SITE);
  await open(TEST_SITE, TEST_FOLDER);
  await open(TEST_FOLDER, file);
  await title(file).click({ timeout: 15_000 });
  await dlg.getByRole('button', { name: /^(Ok|Save)$/ }).click();
  await expect(dlg).toBeHidden({ timeout: 15_000 });
}
