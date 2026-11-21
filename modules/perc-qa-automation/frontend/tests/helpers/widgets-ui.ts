import { Page, Locator, FrameLocator, expect } from '@playwright/test';
import { mouseClick, openLayoutTab, addWidget } from './cms-ui';

/** The page editor's content iframe. */
export const editorFrame = (page: Page): FrameLocator => page.frameLocator('iframe#frame');

/** The widget placed on the open page, by widget definition (e.g. 'percArchiveList'). */
export const widgetOnPage = (page: Page, widgetKey: string): Locator =>
  editorFrame(page).locator(`.perc-widget[widgetdefid="${widgetKey}"]`).first();

/** Layout tab > Save, then waits for the Save button to go away (layout stored). */
export async function saveLayout(page: Page) {
  await page.locator('#perc-save').click();
  await expect(page.locator('#perc-save')).toBeHidden({ timeout: 30_000 }).catch(() => undefined);
}

/** Opens the Content tab with a real mouse click and waits until it is the active tab. */
export async function openContentTab(page: Page) {
  await expect(async () => {
    await mouseClick(page, '#perc-tab-content');
    await expect(page.locator('li:has(> #perc-tab-content)')).toHaveClass(/ui-tabs-active|ui-state-active/, { timeout: 2_000 });
  }).toPass({ timeout: 60_000 });
}

/**
 * Layout tab: selects the widget, clicks the "Configure" icon on it and returns the "Configure Widget Properties" dialog
 * (the widget's preferences: layout, number of entries, locale, ...). Press Ok on it, then saveLayout().
 */
export async function openWidgetConfigure(page: Page, widgetKey: string): Promise<Locator> {
  await openLayoutTab(page);
  const form = page.locator('.ui-dialog', { hasText: 'Configure Widget Properties' });
  await expect(widgetOnPage(page, widgetKey)).toBeVisible({ timeout: 30_000 });
  // selecting the widget shows its icon menu; retry because the menu is attached a moment after the click
  await expect(async () => {
    await widgetOnPage(page, widgetKey).click();
    await editorFrame(page).locator('img[title="Configure"]').first().click({ timeout: 3_000 });
    await expect(form).toBeVisible({ timeout: 3_000 });
  }).toPass({ timeout: 60_000 });
  return form;
}

/** Clicks Ok on the Configure Widget Properties dialog. */
export async function confirmWidgetConfigure(page: Page) {
  await page.locator('.ui-dialog', { hasText: 'Configure Widget Properties' }).getByRole('button', { name: 'Ok' }).click({ timeout: 15_000 });
  await expect(page.locator('.ui-dialog', { hasText: 'Configure Widget Properties' })).toBeHidden({ timeout: 30_000 });
}

/**
 * Content tab: selects the widget and clicks the pencil ("Edit local content") on it. Returns the iframe of the
 * "Edit Widget Content" dialog and the dialog's Save button.
 */
export async function openWidgetContentEditor(page: Page, widgetKey: string): Promise<{ frame: FrameLocator; save: Locator }> {
  await openContentTab(page);
  await widgetOnPage(page, widgetKey).click();
  await editorFrame(page).locator('img[title="Edit local content"]').first().click();
  const frame = page.frameLocator('iframe#edit-widget-content-frame');
  await frame.locator('input[type=text]:visible').first().waitFor({ timeout: 30_000 });
  const dialog = page.locator('.ui-dialog:visible', { hasText: 'Edit Widget Content' });
  return { frame, save: dialog.getByRole('button', { name: 'Save' }) };
}

/**
 * Layout tab: drags the widget from the library into a region and makes sure it really is on the page
 * (the jQuery UI drag occasionally misses its drop target, so the drop is retried).
 */
export async function placeWidget(page: Page, widgetKey: string, region: 'leftsidebar' | 'content' | 'rightsidebar' = 'content') {
  await expect(async () => {
    if ((await editorFrame(page).locator(`.perc-widget[widgetdefid="${widgetKey}"]`).count()) === 0) {
      await addWidget(page, widgetKey, region);
    }
    await expect(widgetOnPage(page, widgetKey)).toBeVisible({ timeout: 4_000 });
  }).toPass({ timeout: 90_000, intervals: [1_000] });
}
