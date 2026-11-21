import { test, expect, Page } from '@playwright/test';
import * as crypto from 'crypto';
import * as fs from 'fs';
import * as path from 'path';
import { loginAsAdmin, selectPage } from '../helpers/cms-ui';
import { PUBLISH_DIR, CMS_LOG, TEST_ASSET_FOLDER } from '../helpers/qa-env';

const OUT = PUBLISH_DIR;
const LOG = CMS_LOG;
const sha = (b: Buffer) => crypto.createHash('sha256').update(b).digest('hex');
const ASSETS = ['excel.xlsx', 'excel97.xls', 'ppt.pptx', 'ppt.ppt', 'word.docx', 'test.pdf'];

async function publishOpenItem(page: Page) {
  const before = fs.readFileSync(LOG).toString().split('Publishing Finished').length;
  await page.getByText('Publishing', { exact: true }).first().click({ timeout: 60_000 });
  await page.locator('li:visible', { hasText: /^Publish$/ }).last().click();
  await expect.poll(() => fs.readFileSync(LOG).toString().split('Publishing Finished').length, { timeout: 120_000 }).toBeGreaterThan(before);
}

test.describe('Pages and assets are published', () => {
  test.setTimeout(300_000);

  test('Pages and assets are published - page: a page is published to the publish location', async ({ page }) => {
    fs.rmSync(`${OUT}/index.html`, { force: true });
    await loginAsAdmin(page);
    await selectPage(page, 'QA-Widgets', 'index.html', true);
    await publishOpenItem(page);
    expect(fs.existsSync(`${OUT}/index.html`), 'page published').toBeTruthy();
    expect(fs.readFileSync(`${OUT}/index.html`, 'utf8')).toContain('<html');
  });

  for (const file of ASSETS) {
    test(`Pages and assets are published - asset: ${file} is published and the published file is byte-identical to the upload`, async ({ page }) => {
      const published = `${OUT}/Assets/${TEST_ASSET_FOLDER}/${file}`;
      fs.rmSync(published, { force: true });
      await loginAsAdmin(page);
      await page.getByText('Assets', { exact: true }).first().click();
      await expect(page.locator('#mcol-path-summary')).toHaveValue('/Assets');
      await page.getByText(TEST_ASSET_FOLDER, { exact: true }).first().click();
      await expect(page.locator('#mcol-path-summary')).toHaveValue(`/Assets/${TEST_ASSET_FOLDER}`);
      await page.getByText(file, { exact: true }).first().dblclick();
      await publishOpenItem(page);

      expect(fs.existsSync(published), 'asset published').toBeTruthy();
      const src = fs.readFileSync(path.join(__dirname, '..', 'fixtures', file));
      const out = fs.readFileSync(published);
      expect({ size: out.length, sha: sha(out) }, 'published bytes equal uploaded bytes').toEqual({ size: src.length, sha: sha(src) });
    });
  }
});
