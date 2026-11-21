import { test, expect } from '@playwright/test';
import * as fs from 'fs';
import { installDir } from '../helpers/qa-env';

const DTS = process.env.DTS_URL ?? 'http://localhost:9981';
const DTS_DIR = process.env.DTS_INSTALL_DIR ?? installDir('dts-new');

test.describe('Install DTS - Standalone new install', () => {
  test('Install DTS - Standalone new install: all DTS web apps are deployed and the server answers on its HTTP port', async ({ page, request }) => {
    // every war must have been exploded by Tomcat
    for (const app of ['feeds', 'perc-comments-services', 'perc-common-ui', 'perc-form-processor',
      'perc-integrations', 'perc-membership-services', 'perc-metadata-services', 'perc-polls-services']) {
      expect(fs.existsSync(`${DTS_DIR}/Deployment/Server/webapps/${app}/WEB-INF`), `${app} deployed`).toBeTruthy();
    }
    const root = await request.get(`${DTS}/`);
    expect(root.status()).toBe(200);
    // service context roots answer with the standard DTS error.html redirect (same as the existing 8.1.8 DTS)
    for (const app of ['feeds', 'perc-metadata-services', 'perc-comments-services', 'perc-polls-services',
      'perc-membership-services', 'perc-form-processor']) {
      const r = await request.get(`${DTS}/${app}/`, { maxRedirects: 0 });
      expect(r.status(), app).toBe(302);
      expect(r.headers()['location']).toContain(`/${app}/error.html`);
    }
    await page.goto(`${DTS}/`);
    await page.screenshot({ path: 'screenshots/install-dts-standalone-new-install-dts-root-page.png' });
  });
});
