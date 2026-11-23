import { test, expect, Page } from '@playwright/test';
import { loginAsAdmin, createPage, publishOpenItem, configurePublishing } from '../helpers/cms-ui';
import { placeWidget, saveLayout, openContentTab, openWidgetContentEditor, openWidgetConfigure, confirmWidgetConfigure } from '../helpers/widgets-ui';
import { DTS_URL, DTS_ADMIN_URL, TEST_FOLDER } from '../helpers/qa-env';

/**
 * Title: writes the title of the page in the chosen format (Heading 1 to 6, Paragraph, Div). By default it shows the page's link text; the widget's
 * editor can switch "Sync with Page Link Text" off and give the page a title of its own. The proof is the PUBLISHED page served by the DTS: the
 * element that holds the title and its text.
 */
const run = Date.now().toString().slice(-7);

/** The element on the published page that holds exactly `text`: its tag name, or null when there is none. */
const tagOf = (page: Page, text: string) =>
  page.getByText(text, { exact: true }).first().evaluate(e => e.tagName.toLowerCase()).catch(() => null);

/** A new page with a Title widget (optionally configured and/or given its own title), published. Returns the title text of the page link and the published URL. */
async function publishTitlePage(page: Page, shot: string, opts: { wrapper?: string; own?: string } = {}) {
  const title = `Title Host ${shot.slice(-9)} ${run}`;
  const host = await createPage(page, title);
  await placeWidget(page, 'percTitle');
  if (opts.wrapper) {
    const form = await openWidgetConfigure(page, 'percTitle');
    await form.locator('select[name="wrapper"]').selectOption(opts.wrapper, { timeout: 10_000 });
    await page.screenshot({ path: `screenshots/${shot}-configure-dialog.png` });
    await confirmWidgetConfigure(page);
  }
  await saveLayout(page);
  if (opts.own) {
    const { frame, save } = await openWidgetContentEditor(page, 'percTitle');
    await frame.locator('#perc-content-edit-sync_link_text').uncheck();
    await frame.locator('#perc-content-edit-text').fill(opts.own);
    await page.screenshot({ path: `screenshots/${shot}-content-editor-own-title.png` });
    await save.click();
    await expect(save).toBeHidden({ timeout: 60_000 });
  }
  await openContentTab(page);
  await publishOpenItem(page);
  return { title, url: `${DTS_URL}/${TEST_FOLDER}/${host}.html` };
}

test.describe('Title', () => {
  test.setTimeout(150_000);

  test('Title: with the default format the page link text is written as a Heading 1', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const { title, url } = await publishTitlePage(page, 'title-default');
    await page.goto(url);
    await expect(page.locator('h1', { hasText: title }), 'page link text in a Heading 1').toHaveText(title);
    await page.screenshot({ path: 'screenshots/title-published-page-default-heading-1.png' });
  });

  // every "Choose format" option
  const formats: [string, string][] = [['h2', 'h2'], ['h3', 'h3'], ['h4', 'h4'], ['h5', 'h5'], ['h6', 'h6'], ['paragraph', 'p'], ['div', 'div']];
  for (const [value, tag] of formats) {
    test(`Title: format "${value}" writes the page link text in a ${tag} element`, async ({ page }) => {
      await loginAsAdmin(page);
      await configurePublishing(page, DTS_ADMIN_URL, 'dts');
      const { title, url } = await publishTitlePage(page, `title-format-${value}`, { wrapper: value });
      await page.goto(url);
      await expect(page.getByText(title, { exact: true }).first(), 'title shown').toBeVisible();
      expect(await tagOf(page, title), `title is in a ${tag}`).toBe(tag);
      await page.screenshot({ path: `screenshots/title-published-page-format-${value}.png` });
    });
  }

  test('Title: a title of its own (Sync with Page Link Text off) replaces the page link text', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const own = `Own title ${run}`;
    const { title, url } = await publishTitlePage(page, 'title-own-text', { own });
    await page.goto(url);
    await expect(page.locator('h1', { hasText: own }), 'the own title is shown in the default Heading 1').toHaveText(own);
    await expect(page.getByText(title, { exact: true }), 'the page link text is not shown').toHaveCount(0);
    await page.screenshot({ path: 'screenshots/title-published-page-own-title.png' });
  });
});
