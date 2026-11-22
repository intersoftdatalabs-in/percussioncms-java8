import { expect, Page, Locator } from '@playwright/test';
import { TEST_ASSET_FOLDER } from './qa-env';

/**
 * The "Select an image" dialog that widget editors open from an image Browse button: a tree of the asset library.
 * Opens it with `browse`, opens Assets > the QA asset folder (each folder opens with the arrow in front of its name and loads its children
 * when it opens, so every step waits until its first child is on screen), picks the image asset by name and presses Ok.
 */
export async function chooseImageFromLibrary(page: Page, browse: Locator, assetName: string) {
  await browse.click({ timeout: 15_000 });
  const dlg = page.locator('.ui-dialog:visible').filter({ hasText: /Select an image|Select image/i });
  await expect(dlg).toBeVisible({ timeout: 15_000 });
  const expander = (name: string) => dlg.locator(`.fancytree-node:has(> .fancytree-title:text-is("${name}")) > .fancytree-expander`);
  const title = (t: string) => dlg.locator('.fancytree-title', { hasText: new RegExp(`^${t}$`) });
  const openFolder = (folder: string, child: string) => expect(async () => {
    if (!(await title(child).isVisible())) await expander(folder).click({ timeout: 3_000 });
    await expect(title(child)).toBeVisible({ timeout: 6_000 });
  }).toPass({ timeout: 45_000 });
  await openFolder('Assets', TEST_ASSET_FOLDER);
  await openFolder(TEST_ASSET_FOLDER, assetName);
  await title(assetName).click({ timeout: 15_000 });
  await dlg.getByRole('button', { name: 'Ok' }).click();
  await expect(dlg).toBeHidden({ timeout: 15_000 });
}
