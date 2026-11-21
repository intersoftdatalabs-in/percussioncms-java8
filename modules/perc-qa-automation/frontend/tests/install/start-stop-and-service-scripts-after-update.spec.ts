import { test, expect } from '@playwright/test';
import { spawn, execSync, spawnSync } from 'child_process';
import { loginAsAdmin } from '../helpers/cms-ui';
import * as path from 'path';
import { CMS_JETTY_DIR, CMS_URL } from '../helpers/qa-env';

const IS_WINDOWS = process.platform === 'win32';
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
    test.skip(!IS_WINDOWS, 'StartJetty.bat / StopJetty.bat are the Windows scripts');
    const url = `${CMS_URL}/`;
    test.skip(!(await down(url, 1000)), `${CMS_URL} is already serving. This scenario starts and stops the CMS itself, so stop the CMS first.`);

    spawn(CMD, ['/c', path.join(JETTY, 'StartJetty.bat')], { cwd: JETTY, detached: true, stdio: 'ignore' }).unref();
    expect(await up(url, 400_000), 'CMS reachable after StartJetty.bat').toBeTruthy();

    await loginAsAdmin(page);
    await expect(page.getByText('Welcome, Admin')).toBeVisible({ timeout: 90_000 });
    await page.screenshot({ path: 'screenshots/start-stop-and-service-scripts-after-update-cms-running-after-start.png' });

    execSync(CMD + ' /c "' + JETTY + '/StopJetty.bat"', { cwd: JETTY, stdio: 'ignore', timeout: 120_000 });
    expect(await down(url, 120_000), 'CMS stopped after StopJetty.bat').toBeTruthy();
  });

  test('Start-stop and Windows service scripts after update: install-jetty-service.bat installs, starts, stops and deletes the Windows service', async ({ page }) => {
    test.skip(!IS_WINDOWS, 'The Windows service scripts only exist for Windows');
    test.skip(process.env.QA_TEST_WINDOWS_SERVICE !== 'true',
      'Opt in with QA_TEST_WINDOWS_SERVICE=true: this creates and removes a Windows service and needs an Administrator (UAC) session');
    const url = `${CMS_URL}/`;
    test.skip(!(await down(url, 1000)), `${CMS_URL} is already serving. Stop the CMS first; the service starts it itself.`);

    const name = `PercussionCMS-QA-${Date.now() % 100000}`;
    const script = path.join(JETTY, 'service', 'install-jetty-service.bat');
    // "< NUL" answers the prompt that install shows about removing a legacy JBoss service
    const run = (command: string) => spawnSync(CMD, ['/c', `"${script}" ${command} ${name} < NUL`],
      { cwd: JETTY, encoding: 'utf8', windowsVerbatimArguments: true, timeout: 180_000 });
    const out = (r: ReturnType<typeof run>) => `${r.stdout ?? ''}${r.stderr ?? ''}`;

    try {
      expect(out(run('install')), 'install command').toContain('succeeded');
      // The service runs as LocalService, so this fails if the installed files are not readable by that account (#326)
      const started = out(run('start'));
      expect(started, 'start command').toContain('succeeded');
      expect(await up(url, 400_000), 'CMS reachable through the Windows service').toBeTruthy();

      await loginAsAdmin(page);
      await expect(page.getByText('Welcome, Admin')).toBeVisible({ timeout: 90_000 });
      await page.screenshot({ path: 'screenshots/start-stop-and-service-scripts-after-update-cms-running-as-windows-service.png' });

      expect(out(run('stop')), 'stop command').toContain('succeeded');
      expect(await down(url, 180_000), 'CMS stopped after the service stop command').toBeTruthy();
    } finally {
      run('stop');
      run('delete');
    }
    const q = spawnSync('sc.exe', ['query', name], { encoding: 'utf8' });
    expect(`${q.stdout}${q.stderr}`, 'service removed').toMatch(/1060|does not exist/i);
  });
});
