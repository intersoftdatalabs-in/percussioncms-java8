import { test, expect } from '@playwright/test';
import * as fs from 'fs';
import { loginAsAdmin } from '../helpers/cms-ui';
import { installDir } from '../helpers/qa-env';

const DIR = process.env.CMS_INSTALL_DIR ?? installDir('cms-new');
const DTS = process.env.DTS_URL ?? 'http://localhost:9982';

test.describe('Install DTS - In place install', () => {
  test('Install DTS - In place install: CMS and DTS coexist in one directory; CMS still works and DTS serves', async ({ page, request }) => {
    expect(fs.existsSync(`${DIR}/Deployment/Server/webapps/feeds/WEB-INF`)).toBeTruthy();
    expect(fs.existsSync(`${DIR}/jetty/base`)).toBeTruthy();

    await loginAsAdmin(page);
    await expect(page.getByText('Welcome, Admin')).toBeVisible({ timeout: 90_000 });
    await expect(page.getByText('Sites', { exact: true }).first()).toBeVisible({ timeout: 60_000 });
    await page.screenshot({ path: 'screenshots/install-dts-in-place-install-cms-dashboard-after-dts-install.png' });

    expect((await request.get(`${DTS}/`)).status()).toBe(200);
    const r = await request.get(`${DTS}/perc-metadata-services/`, { maxRedirects: 0 });
    expect(r.status()).toBe(302);
    await page.goto(`${DTS}/`);
    await page.screenshot({ path: 'screenshots/install-dts-in-place-install-dts-root-page.png' });
  });
});
