/**
 * Shared environment for the structured (TypeScript) specs.
 *
 * Everything is derived from the existing `auth.js` auto-discovery (DEV_PERCUSSION_INSTALL, the CMS/DTS URLs and the
 * generated passwords) so nothing here is tied to one developer's machine. Each value can still be overridden with an
 * environment variable (see README "Test layout").
 */
import * as fs from 'fs';
import * as path from 'path';

// eslint-disable-next-line @typescript-eslint/no-var-requires
const auth = require('./auth');

const norm = (p: string): string => p.replace(/\\/g, '/');

function readProp(file: string, key: string): string | undefined {
  try {
    return fs.readFileSync(file, 'utf8').match(new RegExp(`^${key}=(\\S+)`, 'm'))?.[1];
  } catch {
    return undefined;
  }
}

/** CMS base URL (DEV_PERCUSSION_URL, else discovered from the install). `CMS_URL` overrides for ad-hoc runs. */
export const CMS_URL: string = process.env.CMS_URL ?? auth.BASE_URL;

/** Root of the CMS installation under test (DEV_PERCUSSION_INSTALL). */
export const CMS_INSTALL: string = norm(auth.INSTALL_PATH ?? '');

export const CMS_LOG: string = norm(process.env.CMS_LOG ?? path.join(CMS_INSTALL, 'jetty/base/logs/server.log'));
export const CMS_JETTY_DIR: string = norm(process.env.CMS_JETTY_DIR ?? path.join(CMS_INSTALL, 'jetty'));

/** Folder that holds the sibling installs used by the install scenarios (cms-new, dts-new, ...). */
export const INSTALLS_ROOT: string = norm(process.env.QA_INSTALLS_ROOT ?? path.dirname(CMS_INSTALL));
export const installDir = (name: string): string => norm(path.join(INSTALLS_ROOT, name));

/** Site/folder that the page and widget scenarios create their test pages in. */
export const TEST_SITE: string = process.env.QA_SITE ?? 'QA-Widgets';
export const TEST_FOLDER: string = process.env.QA_FOLDER ?? 'widgets';

/** DTS delivery (HTTP) URL and admin (HTTPS) URL for the in-place DTS next to the CMS. */
const catalina = path.join(CMS_INSTALL, 'Deployment/Server/conf/perc/perc-catalina.properties');
export const DTS_URL: string =
  process.env.DTS_URL ?? auth.DTS_URL ?? `http://localhost:${readProp(catalina, 'http.port') ?? '9980'}`;
export const DTS_ADMIN_URL: string =
  process.env.DTS_ADMIN_URL ?? `https://localhost:${readProp(catalina, 'https.port') ?? '8443'}`;

/** Where the CMS "publish to local file system" output for the test site lands (in-place DTS layout by default). */
export const PUBLISH_DIR: string = norm(
  process.env.PUBLISH_DIR ?? path.join(CMS_INSTALL, 'Deployment/Server', `${TEST_SITE}apps/ROOT`),
);

/** Source fixtures (binary documents) used by the asset scenarios. */
export const FIXTURES_DIR: string = norm(path.join(__dirname, '..', 'fixtures'));

/** Evidence screenshots are written here (git-ignored). */
export const SCREENSHOT_DIR = 'screenshots';
