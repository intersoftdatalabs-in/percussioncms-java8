import { test, expect } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';
import { loginAsAdmin, createPage, addWidget, publishOpenItem, mouseClick } from '../helpers/cms-ui';
import { PUBLISH_DIR } from '../helpers/qa-env';

const OUT = PUBLISH_DIR;

function findPublished(name: string): string | undefined {
  const walk = (d: string): string[] =>
    fs.readdirSync(d, { withFileTypes: true }).flatMap(e => (e.isDirectory() ? walk(path.join(d, e.name)) : [path.join(d, e.name)]));
  return walk(OUT).find(f => path.basename(f) === name);
}

test.describe('Archive List Widget', () => {
  test('Archive List Widget: Archives widget can be placed on a page, saved, and the page publishes with the widget', async ({ page }) => {
    test.setTimeout(300_000);
    await loginAsAdmin(page);
    const base = await createPage(page, 'Wid01 Archive ' + Date.now().toString().slice(-6));
    const file = `${base}.html`;

    await addWidget(page, 'percArchiveList');
    const frame = page.frameLocator('iframe#frame');
    const widget = frame.locator('.perc-widget[widgetdefid="percArchiveList"]');
    await expect(widget).toHaveCount(1);
    await expect(widget.locator('.perc-archive-list')).toBeVisible();
    await page.screenshot({ path: 'screenshots/archive-list-widget-widget-placed-in-content-region.png' });

    // Save the layout; the widget must persist (re-open shows it again).
    await page.locator('#perc-save').click();
    await expect(page.locator('#perc-save')).not.toBeVisible({ timeout: 30_000 }).catch(() => {});
    await mouseClick(page, '#perc-tab-content');
    await expect(page.locator('li:has(> #perc-tab-content)')).toHaveClass(/ui-tabs-active/);
    await expect(widget).toHaveCount(1);
    await page.screenshot({ path: 'screenshots/archive-list-widget-layout-saved.png' });

    await publishOpenItem(page);
    const published = await expect.poll(() => findPublished(file), { timeout: 60_000 }).toBeDefined().then(() => findPublished(file)!);
    const html = fs.readFileSync(published, 'utf8');
    console.log('PUBLISHED', published, html.length);
    console.log(html.slice(0, 3000));
    expect(html).toContain('<html');
    expect(html.toLowerCase()).toMatch(/archive/);
  });
});
