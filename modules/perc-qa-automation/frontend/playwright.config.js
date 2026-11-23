const { defineConfig } = require('@playwright/test');

// HEADLESS defaults to true (CI/Maven). Set HEADLESS=false (e.g. in .env) to watch the browser; SLOWMO=300 slows actions.
const headless = process.env.HEADLESS !== 'false';
const slowMo = Number(process.env.SLOWMO || 0);

// Resolve the CMS base URL from the same auto-discovery the helpers use (DEV_PERCUSSION_URL / install).
const { BASE_URL } = require('./tests/helpers/auth');

module.exports = defineConfig({
  testDir: './tests',
  // login.spec.js plus the structured install/functional/widgets specs (TypeScript or JavaScript).
  testMatch: /.*\.spec\.(js|ts)$/,
  timeout: Number(process.env.TEST_TIMEOUT || 90000),
  retries: 0,
  // The scenarios share one CMS instance and its publish queue, so they run one at a time.
  workers: 1,
  fullyParallel: false,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: process.env.CMS_URL || BASE_URL,
    headless,
    // a step that cannot find its element fails after 30 s, not after the whole test timeout; steps that need longer pass their own timeout
    actionTimeout: Number(process.env.ACTION_TIMEOUT || 30000),
    launchOptions: { slowMo },
    ignoreHTTPSErrors: true,
    viewport: { width: 1440, height: 900 },
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
  },
  projects: [
    {
      // Creates the test site/template/folder when they do not exist yet. Skip it with --no-deps or QA_SKIP_SETUP=true.
      name: 'setup',
      testMatch: /setup\/.*\.setup\.ts$/,
      use: { browserName: 'chromium' },
    },
    {
      name: 'chromium',
      testIgnore: /setup\//,
      dependencies: ['setup'],
      use: { browserName: 'chromium' },
    },
  ],
});
