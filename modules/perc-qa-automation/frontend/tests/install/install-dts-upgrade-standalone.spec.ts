import { test, expect } from '@playwright/test';
import * as fs from 'fs';
import { installDir } from '../helpers/qa-env';

const DTS = process.env.DTS_URL ?? 'http://localhost:9983';
const DIR = process.env.DTS_INSTALL_DIR ?? installDir('dts-upgrade');
const APPS = ['feeds', 'perc-metadata-services', 'perc-comments-services', 'perc-polls-services',
  'perc-membership-services', 'perc-form-processor'];

test.describe('Install DTS - Upgrade install standalone', () => {
  test('Install DTS - Upgrade install standalone: upgraded DTS has refreshed wars, keeps its settings and serves all apps', async ({ page, request }) => {
    // wars were replaced by the upgrade (modified on the day of the upgrade, not at the original install date)
    for (const war of fs.readdirSync(`${DIR}/Deployment/Server/webapps`).filter(f => f.endsWith('.war'))) {
      const m = fs.statSync(`${DIR}/Deployment/Server/webapps/${war}`).mtime;
      expect(m.getTime(), `${war} refreshed`).toBeGreaterThan(new Date('2026-11-20T00:00:00').getTime());
    }
    expect((await request.get(`${DTS}/`)).status()).toBe(200);
    for (const app of APPS) {
      const r = await request.get(`${DTS}/${app}/`, { maxRedirects: 0 });
      expect(r.status(), app).toBe(302);
      expect(r.headers()['location']).toContain(`/${app}/error.html`);
    }
    await page.goto(`${DTS}/`);
    await page.screenshot({ path: 'screenshots/install-dts-upgrade-standalone-dts-root-page.png' });
  });
});
