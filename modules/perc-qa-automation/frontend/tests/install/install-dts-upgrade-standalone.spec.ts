import { test, expect } from '@playwright/test';
import * as fs from 'fs';
import { dirExists, dtsUrlFor, installDir } from '../helpers/qa-env';

// A standalone DTS that was UPGRADED to the build under test. Defaults to <QA_INSTALLS_ROOT>/dts-upgrade; override with DTS_INSTALL_DIR.
const DIR = process.env.DTS_INSTALL_DIR ?? installDir('dts-upgrade');
const DTS = process.env.DTS_URL ?? dtsUrlFor(DIR);
// ISO date/time of the upgrade (e.g. 2026-11-20T00:00:00): the wars must have been replaced after it.
const UPGRADED_AFTER = process.env.QA_UPGRADE_SINCE;
const APPS = ['feeds', 'perc-metadata-services', 'perc-comments-services', 'perc-polls-services',
  'perc-membership-services', 'perc-form-processor'];

test.describe('Install DTS - Upgrade install standalone', () => {
  test('Install DTS - Upgrade install standalone: upgraded DTS has refreshed wars, keeps its settings and serves all apps', async ({ page, request }) => {
    test.skip(!dirExists(`${DIR}/Deployment/Server/webapps`), `No upgraded standalone DTS at ${DIR}; set DTS_INSTALL_DIR to run this scenario`);
    test.skip(!UPGRADED_AFTER, 'Set QA_UPGRADE_SINCE (ISO date/time of the upgrade) so the refreshed wars can be recognised');
    // wars were replaced by the upgrade (modified after the upgrade started, not at the original install date)
    for (const war of fs.readdirSync(`${DIR}/Deployment/Server/webapps`).filter(f => f.endsWith('.war'))) {
      const m = fs.statSync(`${DIR}/Deployment/Server/webapps/${war}`).mtime;
      expect(m.getTime(), `${war} refreshed`).toBeGreaterThan(new Date(UPGRADED_AFTER!).getTime());
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
