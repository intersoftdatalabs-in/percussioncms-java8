import { expect, Page, Locator } from '@playwright/test';
import { TEST_SITE } from './qa-env';
import { placeWidget, openWidgetContentEditor, openContentTab } from './widgets-ui';
import { publishOpenItem, openPageByPath } from './cms-ui';

/**
 * Blogs are created with the New Blog wizard of the dashboard "Blogs" gadget, and the wizard only offers templates that already
 * contain the Blog List / Blog Post widgets. These helpers build those two templates in the Design view, then create the blog
 * and its posts through the gadget (the supported way), so the widgets are exercised exactly as a user would.
 */
export const BLOG_INDEX_TEMPLATE = 'qa-blog-index';
export const BLOG_POST_TEMPLATE = 'qa-blog-post';

/** Design view with the test site opened so that its template cards are listed. */
async function openSiteTemplates(page: Page) {
  await page.goto('/cm/app/?view=design');
  await page.getByText(TEST_SITE, { exact: true }).first().dblclick({ timeout: 60_000 });
  await expect(page.locator('.perc-template-item').first()).toBeVisible({ timeout: 60_000 });
}

const templateCard = (page: Page, name: string): Locator =>
  page.locator('.perc-template-item').filter({ has: page.locator('.perc-template-label', { hasText: new RegExp(`^${name}$`) }) });

/** Actions > Add template > Percussion Templates (Box) and renames the new template to `name`. */
async function addTemplate(page: Page, name: string) {
  const labels = () => page.locator('.perc-template-item .perc-template-label').allInnerTexts();
  const before = await labels();
  // a template left over by an earlier, interrupted run still has the default name: reuse it
  let created = before.find(l => /^Box-Copy/.test(l));
  if (!created) {
    await page.getByText('Actions', { exact: true }).first().click();
    await page.getByText('Add template', { exact: false }).first().click({ timeout: 10_000 });
    const dlg = page.locator('.ui-dialog:visible').filter({ hasText: 'Add Template' });
    await dlg.getByText('Percussion Templates').click();
    await dlg.getByRole('button', { name: 'Save' }).click();
    await expect(async () => expect((await labels()).length).toBeGreaterThan(before.length)).toPass({ timeout: 60_000 });
    created = (await labels()).find(l => !before.includes(l))!;
  }
  // double-clicking the label turns it into an inline edit box
  await page.locator('.perc-template-item .perc-template-label', { hasText: new RegExp(`^${created}$`) }).first().dblclick();
  const input = page.locator('.perc-template-edit-label input, input.perc-template-edit-label').first();
  await expect(input).toBeVisible({ timeout: 15_000 });
  await input.press('Control+A');
  await input.pressSequentially(name, { delay: 30 });
  await input.press('Enter');
  await expect(templateCard(page, name)).toHaveCount(1, { timeout: 30_000 });
}

/** Opens the template editor of `name` (site templates list) on its Layout tab. */
async function openTemplateEditor(page: Page, name: string) {
  await openSiteTemplates(page);
  await templateCard(page, name).locator('button.perc-edit').click();
  await expect(page.getByText(`Editing Template: ${name}`)).toBeVisible({ timeout: 60_000 });
  await page.getByText('Layout', { exact: true }).first().click();
  await expect(page.frameLocator('iframe#frame').locator('#content')).toBeVisible({ timeout: 60_000 });
}

const widgetOnTemplate = (page: Page, widgetKey: string): Locator =>
  page.frameLocator('iframe#frame').locator(`.perc-widget[widgetdefid="${widgetKey}"]`);

/** Makes sure the template `name` exists and has the widget in its content region (adds both when missing) and that it was really saved. */
export async function ensureTemplateWithWidget(page: Page, name: string, widgetKey: string) {
  await openSiteTemplates(page);
  if ((await templateCard(page, name).count()) === 0) await addTemplate(page, name);
  await openTemplateEditor(page, name);
  if ((await widgetOnTemplate(page, widgetKey).count()) > 0) return;
  await placeWidget(page, widgetKey, 'content');
  await page.getByRole('button', { name: 'Save' }).click();
  await expect(page.getByText(/Processing\. Please wait/)).toBeHidden({ timeout: 60_000 });
  // prove the save: reopen the template and look for the widget
  await openTemplateEditor(page, name);
  await expect(widgetOnTemplate(page, widgetKey)).toHaveCount(1, { timeout: 30_000 });
}

export const BLOG_TITLE = 'QA Blog';
export const BLOG_FOLDER = 'qa-blog';

const blogGadgetFrame = (page: Page) => page.frames().find(f => f.url().includes('PercBlogsGadget'));

/** Dashboard with the "Blogs" gadget shown (it is dragged from "Add Dashboard Gadgets" the first time). Returns the gadget's frame. */
async function openBlogsGadget(page: Page) {
  await page.goto('/cm/app/?view=dash');
  await expect(page.getByText('Welcome, Admin')).toBeVisible({ timeout: 90_000 });
  if (!(await expect.poll(() => !!blogGadgetFrame(page), { timeout: 15_000 }).toBeTruthy().then(() => true, () => false))) {
    await page.getByText('Add Dashboard Gadgets').first().click();
    const tile = page.getByText('Blogs', { exact: true }).first();
    await expect(tile).toBeVisible({ timeout: 15_000 });
    const t = (await tile.boundingBox())!;
    await page.mouse.move(t.x + t.width / 2, t.y + t.height / 2);
    await page.mouse.down();
    await page.mouse.move(t.x + 30, t.y + 60, { steps: 5 });
    await page.mouse.move(250, 650, { steps: 25 });
    await page.mouse.up();
    await expect.poll(() => !!blogGadgetFrame(page), { timeout: 30_000 }).toBeTruthy();
  }
  return blogGadgetFrame(page)!;
}

/** Creates the blog "QA Blog" (folder qa-blog) with the New Blog wizard of the Blogs gadget unless the folder is already there. */
export async function ensureBlog(page: Page) {
  await page.goto('/cm/app/?view=dash');
  await page.getByText('Sites', { exact: true }).first().click();
  await page.getByText(TEST_SITE, { exact: true }).first().click();
  if (await page.getByText(BLOG_FOLDER, { exact: true }).first().waitFor({ timeout: 5_000 }).then(() => true, () => false)) return;
  const gadget = await openBlogsGadget(page);
  await expect(async () => {
    if (!(await gadget.locator('#perc-add-blog-button').isVisible())) await gadget.getByText('All Blogs').first().click({ timeout: 3_000 });
    await expect(gadget.locator('#perc-add-blog-button')).toBeVisible({ timeout: 5_000 });
  }).toPass({ timeout: 30_000 });
  await gadget.locator('#perc-add-blog-button').click();
  const dlg = page.locator('.ui-dialog:visible').filter({ hasText: 'New Blog' });
  await dlg.waitFor({ timeout: 20_000 });
  await dlg.getByText('Sites', { exact: true }).first().click();
  await dlg.getByText(TEST_SITE, { exact: true }).first().click({ timeout: 10_000 });
  await dlg.locator('#perc-blog-text-link').pressSequentially(BLOG_TITLE, { delay: 30 });
  await dlg.getByRole('button', { name: 'Next' }).click();
  await expect(dlg.getByText(BLOG_INDEX_TEMPLATE)).toBeVisible({ timeout: 30_000 });
  await expect(dlg.getByText(BLOG_POST_TEMPLATE)).toBeVisible();
  await dlg.getByRole('button', { name: 'Finish' }).click();
  await expect(dlg).toBeHidden({ timeout: 120_000 });
}

/**
 * Adds a post to the blog through the gadget's "new post" button, fills the Blog Post widget content (title is the post title,
 * byline, body text), saves and publishes the post. Returns the post's file name (without .html).
 */
export async function createAndPublishPost(page: Page, title: string, byline: string, bodyText: string): Promise<string> {
  const gadget = await openBlogsGadget(page);
  await expect(async () => {
    if (!(await gadget.locator('#perc-add-post-button').isVisible())) await gadget.getByText(BLOG_TITLE, { exact: true }).first().click({ timeout: 3_000 });
    await expect(gadget.locator('#perc-add-post-button')).toBeVisible({ timeout: 5_000 });
  }).toPass({ timeout: 60_000 });
  const dlg = page.locator('.ui-dialog:visible').filter({ hasText: 'New Post' });
  // the button is shown a moment before its handler is attached: click again until the dialog opens
  await expect(async () => {
    if (!(await dlg.isVisible())) await gadget.locator('#perc-add-post-button').click({ timeout: 3_000 });
    await expect(dlg).toBeVisible({ timeout: 5_000 });
  }).toPass({ timeout: 40_000 });
  await dlg.locator('#perc-page-linktext').pressSequentially(title, { delay: 30 });
  const name = await dlg.locator('#perc-page-name').inputValue();
  expect(name, 'post file name auto-filled from the title').not.toBe('');
  await dlg.getByRole('button', { name: 'Save' }).click();
  await expect(page).toHaveURL(/view=editor.*mode=edit/, { timeout: 90_000 });

  const { frame, save } = await openWidgetContentEditor(page, 'percBlogPost');
  await expect(frame.locator('#perc-content-edit-displaytitle')).toHaveValue(title);
  const body = frame.frameLocator('#ce_postbodypostbody_ifr').locator('body');
  await body.click();
  await body.pressSequentially(bodyText, { delay: 10 });
  await frame.locator('#perc-content-edit-authorname').pressSequentially(byline, { delay: 20 });
  await save.click();
  await expect(save).toBeHidden({ timeout: 30_000 });
  await openContentTab(page);
  await publishOpenItem(page);
  return name;
}

/** Opens the blog's index page in the editor and publishes it (the Blog List widget is on its template). */
export async function publishBlogIndex(page: Page) {
  await openPageByPath(page, [BLOG_FOLDER], 'index.html');
  await openContentTab(page);
  await publishOpenItem(page);
}

/** Everything a blog scenario needs: the two templates with the blog widgets, and the blog itself. Safe to call again. */
export async function ensureBlogInfrastructure(page: Page) {
  await ensureTemplateWithWidget(page, BLOG_INDEX_TEMPLATE, 'percBlogIndexPage');
  await ensureTemplateWithWidget(page, BLOG_POST_TEMPLATE, 'percBlogPost');
  await ensureBlog(page);
}
