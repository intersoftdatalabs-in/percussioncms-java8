import { expect, Page, Frame } from '@playwright/test';
import * as fs from 'fs';

/**
 * Forms submitted on the published pages are stored by the DTS. The dashboard "Forms Tracker" gadget lists the forms with the number of new
 * submissions and can export them as CSV, so it is how the tests see what was really stored.
 */
const trackerFrame = (page: Page): Frame | undefined => page.frames().find(f => /FormTracker/i.test(f.url()));

/** Dashboard with the Forms Tracker gadget shown (dragged from "Add Dashboard Gadgets" the first time). Returns the gadget's frame. */
async function openFormsTracker(page: Page): Promise<Frame> {
  await page.goto('/cm/app/?view=dash');
  await expect(page.getByText('Welcome, Admin')).toBeVisible({ timeout: 90_000 });
  if (!(await expect.poll(() => !!trackerFrame(page), { timeout: 15_000 }).toBeTruthy().then(() => true, () => false))) {
    await page.getByText('Add Dashboard Gadgets').first().click();
    const tile = page.getByText('Forms Tracker', { exact: true }).first();
    await expect(tile).toBeVisible({ timeout: 15_000 });
    const t = (await tile.boundingBox())!;
    await page.mouse.move(t.x + t.width / 2, t.y + t.height / 2);
    await page.mouse.down();
    await page.mouse.move(t.x + 30, t.y + 60, { steps: 5 });
    await page.mouse.move(250, 650, { steps: 25 });
    await page.mouse.up();
    await expect.poll(() => !!trackerFrame(page), { timeout: 30_000 }).toBeTruthy();
  }
  return trackerFrame(page)!;
}

/** The tracker row of the form with this title (the list is paged, so the pages are walked until the form is found). */
async function findFormRow(page: Page, g: Frame, title: string) {
  for (let i = 0; i < 12; i++) {
    const row = g.locator('tr', { hasText: title }).first();
    if (await row.waitFor({ timeout: 4_000 }).then(() => true, () => false)) return row;
    const next = g.locator('a:text-is(">")').first();
    if (!(await next.isVisible().catch(() => false))) break;
    await next.click();
  }
  throw new Error(`form "${title}" is not listed in the Forms Tracker`);
}

/** Number shown in the "New" column for the form (submissions not exported / cleared yet), or 0 when the form is not listed. */
export async function newSubmissionCount(page: Page, title: string): Promise<number> {
  const g = await openFormsTracker(page);
  try {
    const row = await findFormRow(page, g, title);
    return Number((await row.locator('td.perc-new').innerText()).trim());
  } catch { return 0; }
}

/** Row menu > Export: returns the CSV text the DTS exports for the form. */
export async function exportFormData(page: Page, title: string): Promise<string> {
  const g = await openFormsTracker(page);
  const row = await findFormRow(page, g, title);
  const cell = row.locator('td.perc-actions').first();
  await cell.click({ timeout: 10_000 });   // the whole cell opens the row menu
  const download = page.waitForEvent('download', { timeout: 30_000 });
  await g.locator(':text-is("Export"):visible').first().click({ timeout: 10_000 });   // every row has its own hidden menu: take the open one
  const file = await (await download).path();
  return fs.readFileSync(file!, 'utf8');
}
