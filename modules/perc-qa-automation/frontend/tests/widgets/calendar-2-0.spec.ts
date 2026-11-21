import { test, expect, Page } from '@playwright/test';
import { loginAsAdmin, createPage, publishOpenItem, configurePublishing, ensureDeliveryResources } from '../helpers/cms-ui';
import { placeWidget, openWidgetConfigure, confirmWidgetConfigure, openWidgetContentEditor, saveLayout, openContentTab } from '../helpers/widgets-ui';
import { DTS_URL, DTS_ADMIN_URL, TEST_FOLDER } from '../helpers/qa-env';

/**
 * Calendar 2.0: the widget holds a calendar (title, name) and shows the pages that were assigned to that calendar
 * (Edit Metadata > Calendar: select calendar + start/end date). On the PUBLISHED page the browser asks the DTS
 * (perc-metadata-services/metadata/dated/get) for those pages, so the proof is on the page served by the DTS.
 */

const pad = (n: number) => String(n).padStart(2, '0');
/** "YYYY-MM-DD HH:mm" for `days` days from today at the given hour (the format the metadata date picker uses). */
function stamp(days: number, hour: number): string {
  const d = new Date();
  d.setDate(d.getDate() + days);
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(hour)}:00`;
}

/** Actions > Edit Metadata > Calendar: assigns the open page to the calendar and sets its start/end date. */
async function assignToCalendar(page: Page, calendarName: string, start: string, end: string) {
  await page.getByText('Actions', { exact: true }).first().click();
  await page.getByText('Edit Metadata', { exact: true }).first().click();
  const frame = page.frameLocator('iframe#edit-page-metadata-frame');
  const label = frame.locator('.perc-section-label[group="perc-calendar-container"]');
  await expect(async () => {
    if (!(await label.isVisible())) await frame.locator('#perc-content-edit-metadata-link').click({ timeout: 3_000 });
    await expect(label).toBeVisible({ timeout: 3_000 });
  }).toPass({ timeout: 60_000 });
  await label.click();
  await frame.locator('#page_calendar_ms').click();
  await page.waitForTimeout(500);
  // the multiselect list is rendered in the iframe document
  await frame.locator('.ui-multiselect-checkboxes label', { hasText: `QA Calendar ${calendarName.replace('qa-cal-', '')}` }).first().locator('input[type=checkbox]').evaluate(e => (e as HTMLInputElement).click());
  await frame.locator('#page_calendar_ms').click();   // closing the list applies the choice to the form
  await expect(frame.locator('#page_calendar'), 'calendar selected in the metadata form').toHaveValues([calendarName]);
  await frame.locator('#display_calendar_start_date').evaluate((e, v) => { (e as HTMLInputElement).value = v; }, start);
  await frame.locator('#display_calendar_end_date').evaluate((e, v) => { (e as HTMLInputElement).value = v; }, end);
  await page.locator('.ui-dialog:visible', { hasText: 'Edit Metadata' }).getByRole('button', { name: 'Save' }).click();
  await expect(page.locator('.ui-dialog:visible', { hasText: 'Edit Metadata' })).toBeHidden({ timeout: 60_000 });
}

test.describe.serial('Calendar 2.0 Widget', () => {
  test.setTimeout(600_000);
  const run = Date.now().toString().slice(-7);
  const calName = `qa-cal-${run}`;
  const events = [
    { title: `QA Event ${run} Alpha`, days: 1, hour: 9 },
    { title: `QA Event ${run} Bravo`, days: 2, hour: 11 },
    { title: `QA Event ${run} Charlie`, days: 3, hour: 14 },
  ];

  /** Calendar host page: widget with the layout type chosen, calendar asset created in its content editor, then published. */
  async function publishCalendarHost(page: Page, name: string, layout: 'calendar' | 'list', limit: number | null, shot: string, createAsset: boolean) {
    const host = await createPage(page, name);
    await placeWidget(page, 'percCalendarTwo');
    const configure = await openWidgetConfigure(page, 'percCalendarTwo');
    await expect(configure.getByText('Choose layout type')).toBeVisible();
    await configure.locator('select').first().selectOption(layout);
    if (limit !== null) await configure.locator('input[type=text], input[type=number]').first().fill(String(limit)).catch(() => undefined);
    await page.screenshot({ path: `screenshots/${shot}-configure-dialog.png` });
    await confirmWidgetConfigure(page);
    await saveLayout(page);
    const { frame, save } = await openWidgetContentEditor(page, 'percCalendarTwo');
    await frame.locator('#perc-content-edit-calendar_title').fill(`QA Calendar ${run}`);
    await frame.locator('#display_sys_title').fill(calName);
    await page.screenshot({ path: `screenshots/${shot}-content-editor.png` });
    await save.click();
    await expect(save).toBeHidden({ timeout: 30_000 });
    await openContentTab(page);
    await publishOpenItem(page);
    return host;
  }

  test('Calendar 2.0 Widget: pages assigned to the calendar appear in the published Event List with title, date and time', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    await ensureDeliveryResources(page);

    // the calendar itself is created through the widget's content editor on a host page (list layout, limit 2 shown later)
    const host = await publishCalendarHost(page, `Calendar Host ${run}`, 'list', null, 'calendar-2-0-event-list', true);

    for (const ev of events) {
      await createPage(page, ev.title);
      await assignToCalendar(page, calName, stamp(ev.days, ev.hour), stamp(ev.days, ev.hour + 1));
      await openContentTab(page);
      await publishOpenItem(page);
    }

    await page.goto(`${DTS_URL}/${TEST_FOLDER}/${host}.html`);
    const rows = page.locator('.perc-event-list li.perc-event-list-element');
    await expect(rows, 'three events in the published list').toHaveCount(3, { timeout: 60_000 });
    for (let i = 0; i < events.length; i++) {
      await expect(rows.nth(i).locator('.perc-event-title-link')).toHaveText(events[i].title);
      const d = new Date(); d.setDate(d.getDate() + events[i].days);
      await expect(rows.nth(i).locator('.perc-event-date')).toContainText(d.toLocaleString('en-US', { month: 'long' }));
      await expect(rows.nth(i).locator('.perc-event-time')).toContainText(/\d{1,2}:00(AM|PM)/);
    }
    await expect(rows.first()).toHaveClass(/perc-list-first/);
    await expect(rows.last()).toHaveClass(/perc-list-last/);
    await page.screenshot({ path: 'screenshots/calendar-2-0-published-event-list-shows-three-assigned-pages.png' });
  });
});
