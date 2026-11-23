import { test, expect, Page, FrameLocator } from '@playwright/test';
import { loginAsAdmin, createPage, publishOpenItem, configurePublishing } from '../helpers/cms-ui';
import { placeWidget, saveLayout, openContentTab, widgetOnPage, editorFrame, openWidgetConfigure, confirmWidgetConfigure } from '../helpers/widgets-ui';
import { DTS_URL, DTS_ADMIN_URL, TEST_FOLDER } from '../helpers/qa-env';

/**
 * Social Buttons: a row of buttons to share the page (Share mode) or to open the organisation's own pages (Page mode: a link per network). The
 * buttons are written by script in the browser from the choices made in the widget's editor, so the proof is the PUBLISHED page served by the DTS:
 * which buttons there are, in which order, where they link, their colours and where they sit on the page.
 */
const run = Date.now().toString().slice(-7);
const PLATFORMS = ['facebook', 'twitter', 'linkedin', 'pinterest', 'whatsapp', 'email'];   // the networks that can share a page

/** Opens the widget's editor: it has no text box to wait for in Share mode, so it waits for the button type list. */
async function openButtonsEditor(page: Page) {
  await openContentTab(page);
  await widgetOnPage(page, 'percSocialButtons').click();
  await editorFrame(page).locator('img[title="Edit local content"]').first().click();
  const frame = page.frameLocator('iframe#edit-widget-content-frame');
  await frame.locator('#perc-social-button-type').waitFor({ timeout: 60_000 });
  const dialog = page.locator('.ui-dialog:visible', { hasText: 'Edit Widget Content' });
  return { frame, save: dialog.getByRole('button', { name: 'Save' }) };
}

const row = (frame: FrameLocator, platform: string) => frame.locator(`.perc-${platform}-row`);

/** A new page with a Social Buttons widget: Configure through `configure`, the editor through `editor`; published. Returns the published URL. */
async function publishButtonsPage(page: Page, shot: string, configure: ((f: ReturnType<Page['locator']>) => Promise<void>) | null,
                                  editor: (frame: FrameLocator) => Promise<void>): Promise<{ url: string; title: string }> {
  const title = `Social Host ${shot.slice(-9)} ${run}`;
  const host = await createPage(page, title);
  await placeWidget(page, 'percSocialButtons');
  if (configure) {
    const form = await openWidgetConfigure(page, 'percSocialButtons');
    await configure(form);
    await page.screenshot({ path: `screenshots/${shot}-configure-dialog.png` });
    await confirmWidgetConfigure(page);
  }
  await saveLayout(page);
  const { frame, save } = await openButtonsEditor(page);
  await editor(frame);
  await page.screenshot({ path: `screenshots/${shot}-content-editor.png` });
  await save.click();
  await expect(save).toBeHidden({ timeout: 60_000 });
  await openContentTab(page);
  await publishOpenItem(page);
  return { url: `${DTS_URL}/${TEST_FOLDER}/${host}.html`, title };
}

const container = (page: Page) => page.locator('.perc-social-button-container');
const links = (page: Page) => container(page).locator('a');
async function open(page: Page, url: string) {
  await page.goto(url);
  await expect(links(page).first(), 'the buttons are written by the page script').toBeAttached({ timeout: 60_000 });
}
const hrefs = (page: Page) => links(page).evaluateAll(els => els.map(e => e.getAttribute('href') || ''));

test.describe('Social Buttons', () => {
  test.setTimeout(150_000);

  test('Social Buttons: Share mode with every network on writes a share link for the page for each button, inline, with the standard colours', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const { url, title } = await publishButtonsPage(page, 'social-buttons-share-all', null, async frame => {
      await frame.locator('#perc-social-button-type').selectOption('share');
      for (const p of PLATFORMS) await row(frame, p).locator('.perc-social-platform-enabled').check();
    });
    await open(page, url);
    await expect(links(page), 'a button per network').toHaveCount(PLATFORMS.length);
    const all = (await hrefs(page)).join('\n');
    const enc = encodeURIComponent(url);
    expect(all, 'Facebook share link carries the page address').toContain(`https://www.facebook.com/sharer/sharer.php?u=${enc}`);
    expect(all, 'X share link').toContain(`https://x.com/intent/tweet?text=`);
    expect(all, 'X share link carries the page address').toContain(`&url=${enc}`);
    expect(all, 'LinkedIn share link carries the page address and title').toContain(`https://www.linkedin.com/shareArticle?mini=true&url=${enc}&title=${encodeURIComponent(title)}`);
    expect(all, 'Pinterest share link').toContain(`pinterest.com/pin/create/button/?url=${enc}`);
    expect(all, 'WhatsApp share link').toContain('https://api.whatsapp.com/send?text=');
    expect(all, 'e-mail link').toContain(`mailto:someone@example.com?subject=${encodeURIComponent(title)}`);
    await expect(container(page), 'default location is inline').toHaveClass(/perc-social-button-container-inline/);
    for (const a of await links(page).all()) await expect(a, 'opens in a new tab safely').toHaveAttribute('rel', 'noopener noreferrer');
    const colour = await container(page).locator('button.perc-facebook-social-button').evaluate(e => getComputedStyle(e).backgroundColor);
    expect(colour, 'standard colour of Facebook (3b5998)').toBe('rgb(59, 89, 152)');
    await page.screenshot({ path: 'screenshots/social-buttons-published-page-all-share-buttons.png' });
  });

  test('Social Buttons: only the enabled networks get a button and "Move up" changes the order of the buttons', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const { url } = await publishButtonsPage(page, 'social-buttons-order', null, async frame => {
      await frame.locator('#perc-social-button-type').selectOption('share');
      await row(frame, 'facebook').locator('.perc-social-platform-enabled').check();
      await row(frame, 'linkedin').locator('.perc-social-platform-enabled').check();
      for (let i = 0; i < PLATFORMS.length; i++) await row(frame, 'linkedin').locator('.perc-move-button-up').click();   // up to the top row (a click on the top row does nothing)
    });
    await open(page, url);
    await expect(links(page), 'only the two enabled networks').toHaveCount(2);
    const classes = await container(page).locator('button').evaluateAll(els => els.map(e => e.className));
    expect(classes[0], 'LinkedIn is first after Move up').toContain('perc-linkedin-social-button');
    expect(classes[1]).toContain('perc-facebook-social-button');
    await page.screenshot({ path: 'screenshots/social-buttons-published-page-order-linkedin-first.png' });
  });

  test('Social Buttons: Page mode links each button to the address given for it, including YouTube', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const fb = `https://www.facebook.com/qa${run}`;
    const yt = `https://www.youtube.com/@qa${run}`;
    const { url } = await publishButtonsPage(page, 'social-buttons-page-mode', null, async frame => {
      await frame.locator('#perc-social-button-type').selectOption('page');
      await row(frame, 'facebook').locator('.perc-social-platform-enabled').check();
      await row(frame, 'facebook').locator('.perc-social-page-link').fill(fb);
      await row(frame, 'youtube').locator('.perc-social-platform-enabled').check();
      await row(frame, 'youtube').locator('.perc-social-page-link').fill(yt);
    });
    await open(page, url);
    await expect(links(page)).toHaveCount(2);
    expect(await hrefs(page), 'each button links to the address typed for it').toEqual(expect.arrayContaining([fb, yt]));
    await page.screenshot({ path: 'screenshots/social-buttons-published-page-page-mode.png' });
  });

  test('Social Buttons: the grayscale colour scheme and the fixed left position with its vertical offset are applied', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const { url } = await publishButtonsPage(page, 'social-buttons-grayscale-left', async f => {
      await f.locator('select[name="colorScheme"]').selectOption('grayscale', { timeout: 10_000 });
      await f.locator('select[name="desktopButtonLocation"]').selectOption('left', { timeout: 10_000 });
      await f.locator('input[name="desktopVerticalOffset"]').fill('150px');
    }, async frame => {
      await frame.locator('#perc-social-button-type').selectOption('share');
      await row(frame, 'facebook').locator('.perc-social-platform-enabled').check();
    });
    await open(page, url);
    await expect(container(page)).toHaveClass(/perc-social-button-container-left/);
    const style = await container(page).evaluate(e => { const s = getComputedStyle(e); return { position: s.position, top: s.top }; });
    expect(style.position, 'fixed on the left').toBe('fixed');
    expect(style.top, 'vertical offset').toBe('150px');
    const colour = await container(page).locator('button.perc-facebook-social-button').evaluate(e => getComputedStyle(e).backgroundColor);
    expect(colour, 'grayscale colour (666666)').toBe('rgb(102, 102, 102)');
    await page.screenshot({ path: 'screenshots/social-buttons-published-page-grayscale-fixed-left.png' });
  });

  test('Social Buttons: fixed bottom and right positions, the mobile position and size, and the hide options are applied', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const bottom = await publishButtonsPage(page, 'social-buttons-bottom-mobile-right', async f => {
      await f.locator('select[name="desktopButtonLocation"]').selectOption('bottom', { timeout: 10_000 });
      await f.locator('select[name="mobileButtonLocation"]').selectOption('right', { timeout: 10_000 });
      await f.locator('select[name="mobileButtonSize"]').selectOption('1.5em', { timeout: 10_000 });
      await f.locator('input[name="mobileBreakpoint"]').fill('800px');
      await f.locator('input[name="mobileVerticalOffset"]').fill('80px');
    }, async frame => {
      await frame.locator('#perc-social-button-type').selectOption('share');
      await row(frame, 'facebook').locator('.perc-social-platform-enabled').check();
    });
    await open(page, bottom.url);
    await expect(container(page), 'desktop bottom and mobile right classes').toHaveClass(/perc-social-button-container-bottom[\s\S]*perc-social-button-container-mobile-right/);
    expect(await container(page).evaluate(e => getComputedStyle(e).position), 'fixed at the bottom on a wide screen').toBe('fixed');
    await page.setViewportSize({ width: 600, height: 800 });   // at or below the 800px mobile breakpoint
    const mobile = await container(page).evaluate(e => { const s = getComputedStyle(e); return { position: s.position, top: s.top }; });
    expect(mobile.position, 'fixed at the side on a narrow screen').toBe('fixed');
    expect(mobile.top, 'mobile vertical offset').toBe('80px');
    const sizes = await container(page).locator('button.perc-social-button').evaluate(e => ({ own: parseFloat(getComputedStyle(e).fontSize), parent: parseFloat(getComputedStyle(e.parentElement!).fontSize) }));
    expect(sizes.own, 'mobile button size Large (1.5em)').toBeCloseTo(sizes.parent * 1.5, 0);
    await page.screenshot({ path: 'screenshots/social-buttons-published-page-mobile-right.png' });

    await page.setViewportSize({ width: 1440, height: 900 });   // back to a normal window to build the next page in the CMS
    const hidden = await publishButtonsPage(page, 'social-buttons-hide-desktop', async f => {
      await f.locator('input[name="hideDesktop"]').check();
    }, async frame => {
      await frame.locator('#perc-social-button-type').selectOption('share');
      await row(frame, 'facebook').locator('.perc-social-platform-enabled').check();
    });
    await page.setViewportSize({ width: 1440, height: 900 });
    await open(page, hidden.url);
    await expect(container(page), 'hidden on a wide screen').toBeHidden();
    await page.setViewportSize({ width: 600, height: 800 });
    await expect(container(page), 'still shown on a narrow screen').toBeVisible();
  });

  test('Social Buttons: "Hide buttons when <= Mobile breakpoint" hides them on a narrow screen only', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const { url } = await publishButtonsPage(page, 'social-buttons-hide-mobile', async f => {
      await f.locator('input[name="hideMobile"]').check();
    }, async frame => {
      await frame.locator('#perc-social-button-type').selectOption('share');
      await row(frame, 'facebook').locator('.perc-social-platform-enabled').check();
    });
    await page.setViewportSize({ width: 1440, height: 900 });
    await open(page, url);
    await expect(container(page), 'shown on a wide screen').toBeVisible();
    await page.setViewportSize({ width: 600, height: 800 });
    await expect(container(page), 'hidden on a narrow screen (the default breakpoint is 1024px)').toBeHidden();
    await page.screenshot({ path: 'screenshots/social-buttons-published-page-hidden-on-mobile.png' });
  });

  test('Social Buttons: Data push on a button is written to its link so the click can be reported', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const { url } = await publishButtonsPage(page, 'social-buttons-data-push', null, async frame => {
      await frame.locator('#perc-social-button-type').selectOption('share');
      await row(frame, 'facebook').locator('.perc-social-platform-enabled').check();
      await row(frame, 'facebook').locator('.perc-social-data-push-enabled').check();
      await row(frame, 'twitter').locator('.perc-social-platform-enabled').check();
    });
    await open(page, url);
    await expect(container(page).locator('a[data-enablepush="true"]'), 'only the button with data push on').toHaveCount(1);
    await expect(container(page).locator('a[data-enablepush="true"]'), 'push value names the network and mode').toHaveAttribute('data-pushvalue', 'facebook-share');
    await expect(container(page).locator('a[data-enablepush="false"]'), 'the other button has it off').toHaveCount(1);
  });
});
