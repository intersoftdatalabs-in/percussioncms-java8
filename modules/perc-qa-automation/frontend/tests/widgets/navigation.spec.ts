import { test, expect, Page } from '@playwright/test';
import { loginAsAdmin, publishOpenItem, configurePublishing, ensureLevel2Section, addLevel3Section, openPageByPath } from '../helpers/cms-ui';
import { placeWidget, openWidgetConfigure, confirmWidgetConfigure, saveLayout, openContentTab } from '../helpers/widgets-ui';
import { DTS_URL, DTS_ADMIN_URL } from '../helpers/qa-env';

/**
 * Navigation: a menu built by the CMS at publish time from the site's sections (Home, level 2 sections, their level 3 sections). The test builds a
 * real section tree, puts the widget on a level 3 section's landing page and checks the PUBLISHED page served by the DTS: the entries, their links,
 * the nesting and the options chosen in Configure (layout, number of levels, root level, maximum items, title attributes, skip link, label).
 */
const LEVEL2_TITLE = 'QA Crumb Two';
const LEVEL2_FOLDER = 'qa-crumb-two';
const LEVEL2_SECOND_TITLE = 'QA Nav Two';
const LEVEL2_SECOND_FOLDER = 'qa-nav-two';

type Form = ReturnType<Page['locator']>;

/** Creates a level 3 section, puts a Navigation widget on its landing page, configures it, publishes it. Returns the published URL and the section title. */
async function publishNavigationPage(page: Page, run: string, shot: string, configure?: (form: Form) => Promise<void>) {
  const l3Title = `QA N3 ${run}`;
  const l3Folder = `qa-nav-three-${run}`;
  await ensureLevel2Section(page, LEVEL2_TITLE, LEVEL2_FOLDER);
  await addLevel3Section(page, l3Title, l3Folder);
  // the menu is built from PUBLISHED sections: publish the parent's landing page
  await openPageByPath(page, [LEVEL2_FOLDER], 'index.html');
  await openContentTab(page);
  await publishOpenItem(page);
  await openPageByPath(page, [LEVEL2_FOLDER, l3Folder], 'index.html');
  await placeWidget(page, 'percNavBar');
  const form = await openWidgetConfigure(page, 'percNavBar');
  if (configure) await configure(form);
  await page.screenshot({ path: `screenshots/${shot}-configure-dialog.png` });
  await confirmWidgetConfigure(page);
  await saveLayout(page);
  await openContentTab(page);
  await publishOpenItem(page);
  return { url: `${DTS_URL}/${LEVEL2_FOLDER}/${l3Folder}/index.html`, l3Title };
}

// serial: every scenario creates a level 3 section below the shared level 2 section, so they must not run in parallel. The sections are left in the
// QA site (one per scenario and run) because the specs only create content in the dedicated QA site.
test.describe.serial('Navigation', () => {
  test.setTimeout(420_000);
  const run = () => Date.now().toString().slice(-6);

  test('Navigation: the published page lists the sections with links, the level 3 section below its parent, the current section marked, and the default label', async ({ page }) => {
    const r = run();
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const { url, l3Title } = await publishNavigationPage(page, r, 'navigation-defaults');
    await page.goto(url);
    const nav = page.locator('.perc-navbar nav');
    await expect(nav, 'navigation on the published page').toBeAttached({ timeout: 60_000 });
    await expect(nav).toHaveAttribute('aria-label', 'Main Navigation');
    await expect(nav.locator('> ul'), 'default layout is Horizontal rollover').toHaveClass(/sf-menu/);
    await expect(nav.locator('> ul')).toHaveClass(/perc-navbar-sfhorizontal/);
    const l2 = nav.locator('li > a', { hasText: LEVEL2_TITLE }).first();
    await expect(l2, 'level 2 section is listed').toHaveAttribute('href', new RegExp(`/${LEVEL2_FOLDER}/`));
    const l3 = nav.locator('li > a', { hasText: l3Title }).first();
    await expect(l3, 'level 3 section is listed').toHaveAttribute('href', new RegExp(`/${LEVEL2_FOLDER}/qa-nav-three-${r}/`));
    await expect(nav.locator('li', { hasText: LEVEL2_TITLE }).first().locator('ul li a', { hasText: l3Title }), 'level 3 is below its parent').toHaveCount(1);
    await expect(l3.locator('xpath=..'), 'the page being shown is marked as current').toHaveClass(/current/);
    await page.screenshot({ path: 'screenshots/navigation-published-page-defaults.png' });
    // the links work
    const res = await page.request.get(new URL((await l2.getAttribute('href'))!, url).toString());
    expect(res.status(), 'the level 2 link opens').toBe(200);
  });

  const layouts: [string, string, RegExp][] = [
    ['Horizontal', 'perc-navbar-horizontal', /^perc-navbar-horizontal$/],
    ['Vertical', 'perc-navbar-vertical', /^perc-navbar-vertical$/],
    ['Vertical rollover', 'sf-menu sf-vertical perc-navbar-sfvertical', /sf-vertical.*perc-navbar-sfvertical/],
    ['NavBar rollover', 'sf-menu sf-navbar perc-navbar-sfnavbar', /sf-navbar.*perc-navbar-sfnavbar/],
  ];
  for (const [label, value, cls] of layouts) {
    test(`Navigation: Layout "${label}" from Configure is applied to the published menu`, async ({ page }) => {
      await loginAsAdmin(page);
      await configurePublishing(page, DTS_ADMIN_URL, 'dts');
      const { url } = await publishNavigationPage(page, run(), `navigation-layout-${label.toLowerCase().replace(/ /g, '-')}`, async form => {
        await form.locator('select[name="layout"]').selectOption(value, { timeout: 10_000 });
      });
      await page.goto(url);
      const ul = page.locator('.perc-navbar nav > ul');
      await expect(ul).toBeVisible({ timeout: 60_000 });
      await expect(ul, `layout ${label}`).toHaveAttribute('class', cls);
      await page.screenshot({ path: `screenshots/navigation-published-page-layout-${label.toLowerCase().replace(/ /g, '-')}.png` });
    });
  }

  // the levels are counted from Home: level 1 = Home, level 2 = the main sections, level 3 = their sub sections
  test('Navigation: "Maximum number of levels" 1 shows only Home', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const { url } = await publishNavigationPage(page, run(), 'navigation-one-level', async form => {
      await form.locator('input[name="maxlevels"]').fill('1');
    });
    await page.goto(url);
    const nav = page.locator('.perc-navbar nav');
    await expect(nav).toBeAttached({ timeout: 60_000 });
    await expect(nav.locator('a'), 'only the Home entry').toHaveText(['Home']);
    await page.screenshot({ path: 'screenshots/navigation-published-page-one-level.png' });
  });

  test('Navigation: "Maximum number of levels" 2 shows Home and the main sections, without sub menus', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const { url, l3Title } = await publishNavigationPage(page, run(), 'navigation-two-levels', async form => {
      await form.locator('input[name="maxlevels"]').fill('2');
    });
    await page.goto(url);
    const nav = page.locator('.perc-navbar nav');
    await expect(nav).toBeAttached({ timeout: 60_000 });
    await expect(nav.locator('li > a', { hasText: LEVEL2_TITLE }).first()).toBeVisible();
    await expect(nav.locator('ul ul'), 'no sub menus').toHaveCount(0);
    await expect(nav.getByText(l3Title), 'the level 3 section is not listed').toHaveCount(0);
    await page.screenshot({ path: 'screenshots/navigation-published-page-two-levels.png' });
  });

  test('Navigation: "Maximum number of navigation items at the root level" 1 (with Primary node 2) shows one root entry', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    // ensure a second published level 2 section exists so rootlevel = 2 has at least two root entries to truncate on a fresh site
    await ensureLevel2Section(page, LEVEL2_SECOND_TITLE, LEVEL2_SECOND_FOLDER);
    await openPageByPath(page, [LEVEL2_SECOND_FOLDER], 'index.html');
    await openContentTab(page);
    await publishOpenItem(page);
    const { url } = await publishNavigationPage(page, run(), 'navigation-max-root-items', async form => {
      await form.locator('input[name="rootlevel"]').fill('2');   // the root items are the entries at the primary node level; with the default primary node (Home) there is only Home
      await form.locator('input[name="maxRootItems"]').fill('1');
    });
    await page.goto(url);
    const nav = page.locator('.perc-navbar nav');
    await expect(nav).toBeAttached({ timeout: 60_000 });
    await expect(nav.locator('> ul > li'), 'one root entry').toHaveCount(1);
    await page.screenshot({ path: 'screenshots/navigation-published-page-max-root-items-one.png' });
  });

  test('Navigation: "Maximum number of navigation items at each sub level" 1 shows one entry in each sub menu', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const { url } = await publishNavigationPage(page, run(), 'navigation-max-sub-items', async form => {
      await form.locator('input[name="maxSubItems"]').fill('1');
    });
    await page.goto(url);
    const nav = page.locator('.perc-navbar nav');
    await expect(nav).toBeAttached({ timeout: 60_000 });
    const subs = nav.locator('ul ul');
    expect(await subs.count(), 'there are sub menus').toBeGreaterThan(0);
    for (let i = 0; i < await subs.count(); i++) await expect(subs.nth(i).locator('> li'), `sub menu ${i + 1} has one entry`).toHaveCount(1);
    await page.screenshot({ path: 'screenshots/navigation-published-page-max-sub-items-one.png' });
  });

  test('Navigation: "Add title attributes" and the "Navigate to" text are written to every link', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const { url, l3Title } = await publishNavigationPage(page, run(), 'navigation-title-attributes', async form => {
      await form.locator('input[name="addtitleattributes"]').check();
      await form.locator('input[name="titleNavigateToText"]').fill('Go to: ');
    });
    await page.goto(url);
    const link = page.locator('.perc-navbar nav a', { hasText: l3Title }).first();
    await expect(link).toBeAttached({ timeout: 60_000 });   // sub menu entries are only shown on hover
    await expect(link, 'title = the Navigate to text followed by the section title').toHaveAttribute('title', new RegExp(`^Go to:? ?${l3Title}$`));
    await page.screenshot({ path: 'screenshots/navigation-published-page-title-attributes.png' });
  });

  test('Navigation: Skip Link text, target region and tabindex, and the Navigation Label are written to the published page', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const { url } = await publishNavigationPage(page, run(), 'navigation-skip-link-and-label', async form => {
      await form.locator('input[name="generateSkipLink"]').check();
      await form.locator('input[name="skipLinkText"]').fill('Jump to the content');
      await form.locator('input[name="skipLinkRegionId"]').fill('perc-content');
      await form.locator('input[name="tabindexValue"]').fill('0');
      await form.locator('input[name="navLabel"]').fill('Site sections');
    });
    await page.goto(url);
    const skip = page.locator('a.perc-navigation-skiplink');
    await expect(skip, 'skip link').toHaveText('Jump to the content', { timeout: 60_000 });
    await expect(skip).toHaveAttribute('href', '#perc-content');
    await expect(skip).toHaveAttribute('tabindex', '0');
    await expect(page.locator('.perc-navbar nav')).toHaveAttribute('aria-label', 'Site sections');
    await page.screenshot({ path: 'screenshots/navigation-published-page-skip-link-and-label.png' });
  });

  test('Navigation: "Primary node" 2 starts the menu at the level 2 sections instead of the top', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const { url, l3Title } = await publishNavigationPage(page, run(), 'navigation-root-level-two', async form => {
      await form.locator('input[name="rootlevel"]').fill('2');
    });
    await page.goto(url);
    const nav = page.locator('.perc-navbar nav');
    await expect(nav).toBeAttached({ timeout: 60_000 });
    await expect(nav.locator('> ul > li > a', { hasText: 'Home' }), 'Home is no longer listed').toHaveCount(0);
    await expect(nav.locator('> ul > li > a', { hasText: LEVEL2_TITLE }), 'the menu starts with the level 2 section').toHaveCount(1);
    await expect(nav.locator('> ul > li', { hasText: LEVEL2_TITLE }).locator('ul li a', { hasText: l3Title }), 'its level 3 sections are below it').toHaveCount(1);
    await page.screenshot({ path: 'screenshots/navigation-published-page-primary-node-two.png' });
  });
});
