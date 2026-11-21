import { test, expect } from '@playwright/test';
import { spawn, execSync } from 'child_process';
import { loginAsAdmin } from '../helpers/cms-ui';
import * as path from 'path';
import { CMS_JETTY_DIR, CMS_URL } from '../helpers/qa-env';

const CMD = process.env.ComSpec ?? 'C:/Windows/System32/cmd.exe';
const JETTY = process.env.CMS_JETTY_DIR ?? CMS_JETTY_DIR;

async function up(url: string, timeoutMs: number) {
  const end = Date.now() + timeoutMs;
  while (Date.now() < end) {
    try { const r = await fetch(url, { redirect: 'manual' }); if (r.status) return true; } catch { /* not yet */ }
    await new Promise(r => setTimeout(r, 3000));
  }
  return false;
}
async function down(url: string, timeoutMs: number) {
  const end = Date.now() + timeoutMs;
  while (Date.now() < end) {
    try { await fetch(url, { redirect: 'manual' }); } catch { return true; }
    await new Promise(r => setTimeout(r, 3000));
  }
  return false;
}

test.describe('Start-stop and Windows service scripts after update', () => {
  test.setTimeout(600_000);
  test('Start-stop and Windows service scripts after update: StartJetty.bat starts the upgraded CMS and StopJetty.bat stops it cleanly', async ({ page }) => {
    const url = `${CMS_URL}/`;
    expect(await down(url, 1000), `${CMS_URL} must not be serving before the test`).toBeTruthy();

    spawn(CMD, ['/c', path.join(JETTY, 'StartJetty.bat')], { cwd: JETTY, detached: true, stdio: 'ignore' }).unref();
    expect(await up(url, 400_000), 'CMS reachable after StartJetty.bat').toBeTruthy();

    await loginAsAdmin(page);
    await expect(page.getByText('Welcome, Admin')).toBeVisible({ timeout: 90_000 });
    await page.screenshot({ path: 'screenshots/start-stop-and-service-scripts-after-update-cms-running-after-start.png' });

    execSync(CMD + ' /c "' + JETTY + '/StopJetty.bat"', { cwd: JETTY, stdio: 'ignore', timeout: 120_000 });
    expect(await down(url, 120_000), 'CMS stopped after StopJetty.bat').toBeTruthy();
  });
});
