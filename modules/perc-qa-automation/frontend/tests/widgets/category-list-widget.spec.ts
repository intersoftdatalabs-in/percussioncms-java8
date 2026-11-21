import { test, expect, Page } from '@playwright/test';
import { loginAsAdmin, createPage, publishOpenItem, configurePublishing, ensureDeliveryResources } from '../helpers/cms-ui';
import { placeWidget, openWidgetConfigure, confirmWidgetConfigure, openWidgetContentEditor, saveLayout, openContentTab } from '../helpers/widgets-ui';
import { DTS_URL, DTS_ADMIN_URL, TEST_FOLDER, TEST_SITE } from '../helpers/qa-env';

/**
 * Category List Widget: shows the categories (Administration > Categories) with the number of published pages in each.
 * The list is built in the browser from the DTS (perc-metadata-services .../categories/get), so the proof is on the
 * PUBLISHED page served by the DTS. Categories must be published to the DTS first (Administration > Categories > Publish).
 */
const ROOT = 'QA Cat Alpha';
const CHILD = 'QA Cat Alpha Child';

/** Administration > Categories: makes sure ROOT and CHILD exist, then publishes the categories to the (production) DTS. */
async function ensureCategoriesPublished(page: Page) {
  await page.goto('/cm/app/?view=workflow');
  const tab = page.locator('#tabs a, .ui-tabs-nav a').filter({ hasText: /^Categories$/ }).first();
  await expect(tab).toBeVisible({ timeout: 60_000 });
  await tab.click();
  const tree = page.locator('#perc-category-tree');
  await expect(page.locator('#perc-categories-add-category-button')).toBeVisible({ timeout: 30_000 });
  const typeName = async (name: string) => {
    await page.locator('#perc-category-name-field').click();
    await page.keyboard.press('Control+A');
    await page.keyboard.type(name, { delay: 40 });
    await page.locator('#perc-category-save').click();
  };
  if (!((await tree.textContent()) ?? '').includes(ROOT)) {
    await page.locator('#perc-categories-add-category-button').click();
    await typeName(ROOT);
    await expect(tree).toContainText(ROOT, { timeout: 30_000 });
  }
  // children are loaded when the parent is expanded
  const rootNode = tree.locator('.fancytree-node').filter({ has: page.locator('.fancytree-title', { hasText: new RegExp(`^${ROOT}$`) }) }).first();
  if ((await rootNode.locator('.fancytree-expander').isVisible()) && !((await rootNode.getAttribute('class')) ?? '').includes('fancytree-expanded')) {
    await rootNode.locator('.fancytree-expander').click();
    await page.waitForTimeout(1500);
  }
  if (!((await tree.textContent()) ?? '').includes(CHILD)) {
    await tree.locator('.fancytree-title', { hasText: new RegExp(`^${ROOT}$`) }).click();
    await page.locator('#perc-categories-add-child-category-button').click();
    await typeName(CHILD);
    await expect.poll(async () => (await tree.textContent()) ?? '', { timeout: 30_000 }).toContain(CHILD);
  }
  await page.screenshot({ path: 'screenshots/category-list-widget-administration-categories-tree.png' });
  await page.locator('#perc-category-site-dropdown').selectOption({ label: TEST_SITE });
  await page.locator('#perc-categories-publish-dropdown').click();
  await page.locator('#perc-categories-publish-production').click();
  const err = page.locator('.ui-dialog:visible', { hasText: 'Publish Error' });
  await page.waitForTimeout(4_000);
  expect(await err.count(), 'categories published to the DTS without an error dialog').toBe(0);
}

/** Actions > Edit Metadata > Tags and Categories: ticks the given categories on the open page and saves. */
async function assignCategories(page: Page, names: string[]) {
  await expect(async () => {
    await page.getByText('Actions', { exact: true }).first().click();
    await page.getByText('Edit Metadata', { exact: true }).first().click({ timeout: 3_000 });
    await expect(page.locator('iframe#edit-page-metadata-frame')).toBeVisible({ timeout: 5_000 });
  }).toPass({ timeout: 60_000 });
  const frame = page.frameLocator('iframe#edit-page-metadata-frame');
  const label = frame.locator('.perc-section-label[group="perc-tagCat-container"]');
  await expect(async () => {
    if (!(await label.isVisible())) await frame.locator('#perc-content-edit-metadata-link').click({ timeout: 3_000 });
    await expect(label).toBeVisible({ timeout: 3_000 });
  }).toPass({ timeout: 60_000 });
  await label.click();
  for (const n of names) {
    await frame.locator(`#page_categories_tree-tree span.fancytree-node:has(> span.fancytree-title:text-is("${n}")) > span.fancytree-checkbox`).click();
  }
  await page.locator('.ui-dialog:visible', { hasText: 'Edit Metadata' }).getByRole('button', { name: 'Save' }).click();
  await expect(page.locator('.ui-dialog:visible', { hasText: 'Edit Metadata' })).toBeHidden({ timeout: 60_000 });
}

test.describe('Category List Widget', () => {
  test.setTimeout(600_000);

  test('Category List Widget: published page lists the categories with the number of pages in each, child nested under its parent', async ({ page }) => {
    const run = Date.now().toString().slice(-7);
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    await ensureDeliveryResources(page);
    await ensureCategoriesPublished(page);

    // pages in the categories: one in the parent, two in the child
    const sources = [
      { n: 'One', cats: [ROOT] },
      { n: 'Two', cats: [CHILD] },
      { n: 'Three', cats: [CHILD] },
    ];
    for (const s of sources) {
      await createPage(page, `Category Source ${run} ${s.n}`);
      await assignCategories(page, s.cats);
      await openContentTab(page);
      await publishOpenItem(page);
    }

    const host = await createPage(page, `Category Host ${run}`);
    await placeWidget(page, 'percCategoryList');
    const configure = await openWidgetConfigure(page, 'percCategoryList');
    await expect(configure.getByText('Display').first()).toBeVisible();
    await expect(configure.getByText('Maximum number of categories to show')).toBeVisible();
    await configure.locator('select').first().selectOption('expanded');
    await page.screenshot({ path: 'screenshots/category-list-widget-configure-dialog-display-and-maximum.png' });
    await confirmWidgetConfigure(page);
    await saveLayout(page);
    const { frame, save } = await openWidgetContentEditor(page, 'percCategoryList');
    await frame.locator('input[name="displaytitle"]').first().fill(`Categories ${run}`);
    await frame.locator('.fancytree-title', { hasText: /^Sites$/ }).click();
    await frame.locator('.fancytree-title', { hasText: new RegExp(`^${TEST_SITE}$`) }).click();
    await page.screenshot({ path: 'screenshots/category-list-widget-content-editor-criteria.png' });
    await save.click();
    await expect(save).toBeHidden({ timeout: 30_000 });
    await openContentTab(page);
    await publishOpenItem(page);

    // what the DTS service the widget calls holds for the same site
    const res = await page.request.post(`${DTS_URL}/perc-metadata-services/metadata/categories/get`, { data: { criteria: ["type = 'page'", `site = '${TEST_SITE}'`] } });
    expect(res.ok(), 'DTS category service answers').toBeTruthy();
    const tree: any[] = await res.json();
    const alpha = tree[0]?.children?.find((c: any) => c.category === ROOT);
    const alphaChild = alpha?.children?.find((c: any) => c.category === CHILD);
    expect(alpha, `${ROOT} is known to the DTS`).toBeTruthy();
    expect(alphaChild, `${CHILD} is known to the DTS under ${ROOT}`).toBeTruthy();
    expect(alphaChild.count.first, 'both pages put in the child category are counted by the DTS').toBeGreaterThanOrEqual(2);

    await page.goto(`${DTS_URL}/${TEST_FOLDER}/${host}.html`);
    await expect(page.locator('.perc-category-title')).toHaveText(`Categories ${run}`);
    // list: Categories > QA Cat Alpha (n) > QA Cat Alpha Child (n), each count equal to what the DTS holds
    const rootLi = page.locator('.perc-category-list-expanded > ul > li.perc-category-element').first();
    await expect(rootLi.locator('> a'), 'tree root').toHaveText('Categories', { timeout: 60_000 });
    const alphaLi = rootLi.locator('> ul > li.perc-category-element').filter({ has: page.locator(`> a[title^="${ROOT} ("]`) });
    await expect(alphaLi.locator('> a'), 'parent category with the DTS page count').toHaveText(`${ROOT} (${alpha.count.first})`);
    await expect(alphaLi.locator('> ul > li.perc-category-element > a'), 'child category nested under its parent with the DTS page count').toHaveText(`${CHILD} (${alphaChild.count.first})`);
    await page.screenshot({ path: 'screenshots/category-list-widget-published-page-shows-parent-and-child-categories-with-counts.png' });
  });

  test('Category List Widget: Collapsible display shows the category tree as a collapsible tree on the published page', async ({ page }) => {
    const run = Date.now().toString().slice(-7);
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    await ensureDeliveryResources(page);
    await ensureCategoriesPublished(page);   // categories and the pages of the first test are already in the DTS

    const host = await createPage(page, `Category Collapsible Host ${run}`);
    await placeWidget(page, 'percCategoryList');
    const configure = await openWidgetConfigure(page, 'percCategoryList');
    await configure.locator('select').first().selectOption('collapsible');
    await page.screenshot({ path: 'screenshots/category-list-widget-collapsible-configure-dialog.png' });
    await confirmWidgetConfigure(page);
    await saveLayout(page);
    const { frame, save } = await openWidgetContentEditor(page, 'percCategoryList');
    await frame.locator('input[name="displaytitle"]').first().fill(`Collapsible ${run}`);
    await frame.locator('.fancytree-title', { hasText: /^Sites$/ }).click();
    await frame.locator('.fancytree-title', { hasText: new RegExp(`^${TEST_SITE}$`) }).click();
    await save.click();
    await expect(save).toBeHidden({ timeout: 30_000 });
    await openContentTab(page);
    await publishOpenItem(page);

    await page.goto(`${DTS_URL}/${TEST_FOLDER}/${host}.html`);
    await expect(page.locator('.perc-category-title')).toHaveText(`Collapsible ${run}`);
    const tree = page.locator('.perc-category-list-collapsible');
    await expect(tree.locator('.fancytree-container'), 'rendered as a fancytree').toBeVisible({ timeout: 60_000 });
    await expect(tree.locator('.fancytree-title', { hasText: new RegExp(`^${ROOT} \\(\\d+\\)$`) })).toBeVisible();
    const childTitle = tree.locator('.fancytree-title', { hasText: new RegExp(`^${CHILD} \\(\\d+\\)$`) });
    await expect(childTitle, 'child is collapsed under its parent until expanded').toHaveCount(0);
    await tree.locator('span.fancytree-node:has(> span.fancytree-title:text-matches("^' + ROOT + ' \\\\(")) > span.fancytree-expander').click();
    await expect(childTitle, 'child shown after expanding the parent').toBeVisible();
    await page.screenshot({ path: 'screenshots/category-list-widget-collapsible-published-page-shows-category-tree.png' });
  });
});
