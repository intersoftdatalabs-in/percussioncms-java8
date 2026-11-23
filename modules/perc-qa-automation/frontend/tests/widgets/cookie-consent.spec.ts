import { test, expect, Page } from '@playwright/test';
import { loginAsAdmin, createPage, publishOpenItem, configurePublishing } from '../helpers/cms-ui';
import { placeWidget, saveLayout, openContentTab, widgetOnPage, editorFrame, openWidgetConfigure, confirmWidgetConfigure } from '../helpers/widgets-ui';
import { DTS_URL, DTS_ADMIN_URL, TEST_FOLDER, TEST_SITE } from '../helpers/qa-env';

/**
 * Cookie Consent: a banner that asks the visitor to accept or deny cookies, with the consent text typed in the widget's editor. It runs in the
 * browser on the PUBLISHED page served by the DTS: Accept stores the consent in a cookie and reports it to the DTS, Deny stores a deny cookie, and
 * in both cases the banner does not come back on the next visit. The proof is the published page: the banner, its position, the buttons and the cookies.
 */
const run = Date.now().toString().slice(-7);

/** A new page with a Cookie Consent widget (optionally configured) whose consent text is typed in the editor; published. Returns the published URL. */
async function publishConsentPage(page: Page, shot: string, text: string, configure?: (f: ReturnType<Page['locator']>) => Promise<void>,
                              extra?: (frame: ReturnType<Page['frameLocator']>) => Promise<void>): Promise<string> {
  const host = await createPage(page, `Consent Host ${shot.slice(-9)} ${run}`);
  await placeWidget(page, 'percCookieConsent');
  if (configure) {
    const form = await openWidgetConfigure(page, 'percCookieConsent');
    await configure(form);
    await page.screenshot({ path: `screenshots/${shot}-configure-dialog.png` });
    await confirmWidgetConfigure(page);
  }
  await saveLayout(page);
  await openContentTab(page);
  await widgetOnPage(page, 'percCookieConsent').click();
  await editorFrame(page).locator('img[title="Edit local content"]').first().click();
  const frame = page.frameLocator('iframe#edit-widget-content-frame');
  await expect(frame.locator('.tox-tinymce'), 'the editor is shown').toBeVisible({ timeout: 60_000 });
  await frame.frameLocator('.tox-edit-area__iframe').locator('body').click();
  await page.keyboard.type(text);
  if (extra) await extra(frame);
  await page.screenshot({ path: `screenshots/${shot}-content-editor-consent-text.png` });
  const dialog = page.locator('.ui-dialog:visible', { hasText: 'Edit Widget Content' });
  await dialog.getByRole('button', { name: 'Save' }).click();
  await expect(dialog).toBeHidden({ timeout: 60_000 });
  await openContentTab(page);
  await publishOpenItem(page);
  return `${DTS_URL}/${TEST_FOLDER}/${host}.html`;
}

/** "Privacy / Cookie policy" fields: Browse > "Select a page" tree: Sites > site > folder > the page (by file name) > Ok. Each folder opens with the arrow in front of its name; the arrow is clicked once and the children are waited for (a second click would close it again). */
async function choosePolicyPage(page: Page, frame: ReturnType<Page['frameLocator']>, browseIndex: 0 | 1, pageName: string) {
  await frame.getByRole('button', { name: 'Browse' }).nth(browseIndex).click({ timeout: 15_000 });
  const dlg = page.locator('.ui-dialog:visible').filter({ hasText: /Select a page/i });
  await expect(dlg).toBeVisible({ timeout: 15_000 });
  const title = (t: string) => dlg.locator('.fancytree-title', { hasText: new RegExp(`^${t}$`) });
  const open = async (folder: string, child: string) => {
    // a second chooser opens with the tree already open at the page chosen before: only click the arrow of a folder that is still closed
    if (!(await title(child).first().isVisible())) {
      await dlg.locator(`.fancytree-node:has(> .fancytree-title:text-is("${folder}")) > .fancytree-expander`).click({ timeout: 15_000 });
    }
    await expect(title(child), `${child} is listed under ${folder}`).toBeVisible({ timeout: 30_000 });
  };
  const file = `${pageName}\\.html`;
  await open('Sites', TEST_SITE);
  await open(TEST_SITE, TEST_FOLDER);
  await open(TEST_FOLDER, file);
  await title(file).click({ timeout: 15_000 });
  await dlg.getByRole('button', { name: 'Ok' }).click();
  await expect(dlg).toBeHidden({ timeout: 15_000 });
}

const banner = (page: Page) => page.locator('#perc-cookie-consent');
const cookie = async (page: Page, name: string) => (await page.context().cookies()).find(c => c.name === name)?.value;

test.describe('Cookie Consent', () => {
  test.setTimeout(150_000);

  test('Cookie Consent: the banner shows the consent text at the bottom with Accept and Deny; Accept stores the consent, reports it to the DTS and the banner stays away on the next visit', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const text = `We use cookies ${run}`;
    const url = await publishConsentPage(page, 'cookie-consent-accept', text);
    await page.goto(url);
    await expect(banner(page), 'banner shown').toBeVisible({ timeout: 60_000 });
    await expect(banner(page).locator('.perc-consent-msg'), 'consent text').toContainText(text);
    await expect(banner(page), 'default position is the bottom').toHaveClass(/perc-consent-bottom/);
    await expect(banner(page).locator('#accept-btn')).toBeVisible();
    await expect(banner(page).locator('#deny-btn')).toBeVisible();
    await expect(banner(page).locator('.perc-policy-links a'), 'no policy links until policy pages are chosen').toHaveCount(0);
    await page.screenshot({ path: 'screenshots/cookie-consent-published-page-banner-bottom.png' });

    const report = page.waitForRequest(r => r.method() === 'POST' && /consent/i.test(r.url()), { timeout: 30_000 });
    await banner(page).locator('#accept-btn').click();
    const req = await report;
    expect(req.postData() ?? '', 'the consent report names the site and the opt-in').toMatch(/optIn/);
    await expect(banner(page), 'banner is hidden after Accept').toBeHidden({ timeout: 10_000 });
    expect(await cookie(page, 'percCookieConsent'), 'consent cookie').toBe('true');
    await page.reload();
    await expect(banner(page), 'banner does not come back after a reload').toBeHidden();
  });

  test('Cookie Consent: Deny stores the deny cookie, hides the banner and does not report anything to the DTS', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishConsentPage(page, 'cookie-consent-deny', `Deny test ${run}`);
    await page.goto(url);
    await expect(banner(page)).toBeVisible({ timeout: 60_000 });
    const reported: string[] = [];
    page.on('request', r => { if (r.method() === 'POST' && /consent/i.test(r.url())) reported.push(r.url()); });
    await banner(page).locator('#deny-btn').click();
    await expect(banner(page), 'banner is hidden after Deny').toBeHidden({ timeout: 10_000 });
    expect(await cookie(page, 'deny_cookie'), 'deny cookie').toBe('true');
    expect(await cookie(page, 'percCookieConsent'), 'no consent cookie').toBeUndefined();
    expect(reported, 'nothing reported to the DTS').toEqual([]);
    await page.reload();
    await expect(banner(page), 'banner does not come back after a reload').toBeHidden();
  });

  test('Cookie Consent: position Top, the aria label and the role from Configure are written to the banner', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishConsentPage(page, 'cookie-consent-top-aria', `Top banner ${run}`, async f => {
      await f.locator('select[name="overlayPosition"]').selectOption('top', { timeout: 10_000 });
      await f.locator('input[name="ariaLabel"]').fill(`cookie consent ${run}`);
      await f.locator('input[name="ariaRole"]').fill('dialog');
    });
    await page.goto(url);
    await expect(banner(page)).toBeVisible({ timeout: 60_000 });
    await expect(banner(page), 'position top').toHaveClass(/perc-consent-top/);
    await expect(banner(page), 'aria label').toHaveAttribute('aria-label', `cookie consent ${run}`);
    await expect(banner(page), 'role').toHaveAttribute('role', 'dialog');
    const box = await banner(page).boundingBox();
    expect(box!.y, 'banner sits at the top of the window').toBeLessThan(5);
    await page.screenshot({ path: 'screenshots/cookie-consent-published-page-banner-top.png' });
  });

  test('Cookie Consent: the privacy policy and cookie policy pages chosen in the editor are linked with the link texts from Configure', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const names: string[] = [];
    const titles = [`Consent Privacy Page ${run}`, `Consent Cookies Page ${run}`];
    for (let i = 0; i < 2; i++) {
      names.push(await createPage(page, titles[names.length]));
      await openContentTab(page);
      await publishOpenItem(page);
    }
    const url = await publishConsentPage(page, 'cookie-consent-policy-links', `Policy links ${run}`, async f => {
      await f.locator('input[name="privacyPolicyLinkText"]').fill('Our privacy rules');
      await f.locator('input[name="cookiePolicyLinkText"]').fill('Our cookie rules');
    }, async frame => {
      await choosePolicyPage(page, frame, 0, names[0]);
      await choosePolicyPage(page, frame, 1, names[1]);
    });
    await page.goto(url);
    await expect(banner(page)).toBeVisible({ timeout: 60_000 });
    const links = banner(page).locator('.perc-policy-links a');
    await expect(links, 'both policy links').toHaveCount(2);
    await expect(links.nth(0), 'privacy policy link text').toHaveText('Our privacy rules');
    await expect(links.nth(0), 'privacy policy link address').toHaveAttribute('href', new RegExp(`${names[0]}\.html$`));
    await expect(links.nth(1), 'cookie policy link text').toHaveText('Our cookie rules');
    await expect(links.nth(1), 'cookie policy link address').toHaveAttribute('href', new RegExp(`${names[1]}\.html$`));
    await page.screenshot({ path: 'screenshots/cookie-consent-published-page-policy-links.png' });
  });
});
