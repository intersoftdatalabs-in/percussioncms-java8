import { test, expect, Page } from '@playwright/test';
import * as crypto from 'crypto';
import * as fs from 'fs';
import * as path from 'path';
import { loginAsAdmin } from '../helpers/cms-ui';
import { CMS_URL, TEST_ASSET_FOLDER } from '../helpers/qa-env';

const FIXTURES = ['excel.xlsx', 'excel97.xls', 'ppt.pptx', 'ppt.ppt', 'word.docx', 'test.pdf'];
const sha = (b: Buffer) => crypto.createHash('sha256').update(b).digest('hex');

async function openFolder(page: Page, name: string) {
  await loginAsAdmin(page);
  await expect(page.getByText('Welcome, Admin')).toBeVisible({ timeout: 90_000 });
  await page.getByText('Assets', { exact: true }).first().click();
  await page.getByText(name, { exact: true }).first().click();
}

test.describe('Binary assets upload', () => {
  test.setTimeout(240_000);
  for (const file of FIXTURES) {
    test(`Binary assets upload: upload ${file} as a File asset and download it back unchanged`, async ({ page }) => {
      const src = path.join(__dirname, '..', 'fixtures', file);
      const title = `QA-${file}-${Date.now()}`;
      await openFolder(page, TEST_ASSET_FOLDER);

      await expect(page.locator('#mcol-path-summary')).toHaveValue(`/Assets/${TEST_ASSET_FOLDER}`);
      await page.locator('#mcol-new-asset.ui-enabled').click();
      await page.locator('text=/^File$/ >> visible=true').first().click({ timeout: 60_000 });
      await page.getByRole('button', { name: 'Next' }).click();

      const frame = page.frameLocator('iframe[src*="psx_cepercFileAsset"]');
      await frame.locator('input[name="linktext"], input[type="text"]').first().fill(title);
      await frame.locator('input[type="file"]').setInputFiles(src);
      await page.getByRole('button', { name: 'Save' }).click();

      await page.waitForURL(/view=editAsset.*name=/, { timeout: 60_000 });
      const assetName = new URL(page.url()).searchParams.get('name')!;   // CMS names the asset after the uploaded file
      expect(assetName.toLowerCase()).toContain(path.parse(file).name.toLowerCase());
      const item = page.locator('#perc-finder-container, body').getByText(assetName, { exact: true }).first();
      await page.screenshot({ path: `screenshots/binary-assets-upload-${file}-saved-as-asset.png` });

      await expect(item).toBeVisible({ timeout: 60_000 });
      await page.screenshot({ path: `screenshots/binary-assets-upload-${file}-saved-as-asset.png` });

      // "Download File" in the finder is FSFile-only by design; the stored binary is reached through the editor's Preview File link
      const editor = page.frameLocator('iframe[src*="psx_cepercFileAsset"]');
      const href = await editor.getByText('Preview File').first().getAttribute('href', { timeout: 60_000 });
      expect(href, 'Preview File link present').toBeTruthy();
      const resp = await page.request.get(new URL(href!, `${CMS_URL}/Rhythmyx/psx_cepercFileAsset/`).toString());
      expect(resp.status()).toBe(200);
      const body = await resp.body();
      const outDir = path.join(process.cwd(), 'test-results');
      fs.mkdirSync(outDir, { recursive: true });
      fs.writeFileSync(path.join(outDir, `dl-${file}`), body);
      expect(sha(body), 'stored bytes equal uploaded bytes').toBe(sha(fs.readFileSync(src)));
    });
  }
});
