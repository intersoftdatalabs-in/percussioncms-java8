import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';
import { test, expect, Page, Locator } from '@playwright/test';
import { loginAsAdmin, createPage, publishOpenItem, configurePublishing, ensureDeliveryResources, reopenPageInEditor } from '../helpers/cms-ui';
import { placeWidget, openWidgetConfigure, confirmWidgetConfigure, openWidgetContentEditor, saveLayout, openContentTab } from '../helpers/widgets-ui';
import { DTS_URL, DTS_ADMIN_URL, TEST_FOLDER } from '../helpers/qa-env';

/**
 * Calendar 2.0 options. One calendar with three events (assigned in Edit Metadata > Calendar) is shown by ONE host page that is configured again for
 * every scenario (Configure > save > publish), and the PUBLISHED page served by the DTS is checked: the Event List options (maximum, title tag, calendar
 * icon, date and time with their formats) and the Calendar options (view type, detail level, start day, language, time format, toggle buttons).
 */
process.env.QA_CAL2_RUN ??= Date.now().toString().slice(-7);   // shared with the workers Playwright restarts after a failed scenario
const run = process.env.QA_CAL2_RUN;
const calName = `qa-cal-${run}`;
const state = path.join(os.tmpdir(), `qa-cal2-${run}.json`);
const pad = (n: number) => String(n).padStart(2, '0');
const stamp = (days: number, hour: number) => {
  const d = new Date(); d.setDate(d.getDate() + days);
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(hour)}:00`;
};
const events = [
  { title: `QA Opt Event ${run} Alpha`, days: 1, hour: 9 },
  { title: `QA Opt Event ${run} Bravo`, days: 2, hour: 11 },
  { title: `QA Opt Event ${run} Charlie`, days: 3, hour: 14 },
];

async function assignToCalendar(page: Page, start: string, end: string) {
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
  await frame.locator('.ui-multiselect-checkboxes label', { hasText: `QA Calendar ${run}` }).first().locator('input[type=checkbox]').evaluate(e => (e as HTMLInputElement).click());
  await frame.locator('#page_calendar_ms').click();
  await expect(frame.locator('#page_calendar'), 'calendar selected in the metadata form').toHaveValues([calName]);
  await frame.locator('#display_calendar_start_date').evaluate((e, v) => { (e as HTMLInputElement).value = v; }, start);
  await frame.locator('#display_calendar_end_date').evaluate((e, v) => { (e as HTMLInputElement).value = v; }, end);
  await page.locator('.ui-dialog:visible', { hasText: 'Edit Metadata' }).getByRole('button', { name: 'Save' }).click();
  await expect(page.locator('.ui-dialog:visible', { hasText: 'Edit Metadata' })).toBeHidden({ timeout: 60_000 });
}

/** Creates the calendar (in the widget's content editor of a host page), the three events, and remembers the host. */
async function ensureCalendar(page: Page) {
  if (fs.existsSync(state)) return;
  await ensureDeliveryResources(page);
  const host = await createPage(page, `Calendar Options Host ${run}`);
  await placeWidget(page, 'percCalendarTwo');
  await saveLayout(page);
  const { frame, save } = await openWidgetContentEditor(page, 'percCalendarTwo');
  await frame.locator('#perc-content-edit-calendar_title').fill(`QA Calendar ${run}`);
  await frame.locator('#display_sys_title').fill(calName);
  await save.click();
  await expect(save).toBeHidden({ timeout: 30_000 });
  await openContentTab(page);
  await publishOpenItem(page);
  const editUrl = page.url();
  for (const ev of events) {
    await createPage(page, ev.title);
    await assignToCalendar(page, stamp(ev.days, ev.hour), stamp(ev.days, ev.hour + 1));
    await openContentTab(page);
    await publishOpenItem(page);
  }
  fs.writeFileSync(state, JSON.stringify({ host, editUrl }));
}

/** Opens the host page again, sets the options in Configure, saves and publishes. Returns the published URL. */
async function configureHost(page: Page, shot: string, set: (f: Locator) => Promise<void>): Promise<string> {
  const { host, editUrl } = JSON.parse(fs.readFileSync(state, 'utf8'));
  let form!: Locator;
  // reopening a published page in the editor is sometimes slow to show the widget: open it again once before giving up
  for (let attempt = 1; ; attempt++) {
    try {
      await reopenPageInEditor(page, editUrl, host);
      form = await openWidgetConfigure(page, 'percCalendarTwo');
      break;
    } catch (e) {
      if (attempt >= 2) throw e;
    }
  }
  await set(form);
  await page.screenshot({ path: `screenshots/${shot}-configure-dialog.png` });
  await confirmWidgetConfigure(page);
  await saveLayout(page);
  await openContentTab(page);
  await publishOpenItem(page);
  return `${DTS_URL}/${TEST_FOLDER}/${host}.html`;
}

const sel = (f: Locator, name: string, value: string) => f.locator(`select[name="${name}"]`).selectOption(value, { timeout: 10_000 });
const txt = (f: Locator, name: string, value: string) => f.locator(`input[name="${name}"]`).fill(value);
const chk = (f: Locator, name: string, on: boolean) => on ? f.locator(`input[name="${name}"]`).check() : f.locator(`input[name="${name}"]`).uncheck();

test.describe('Calendar 2.0 options', () => {
  test.setTimeout(600_000);

  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    await ensureCalendar(page);
  });

  test('Calendar 2.0 Event List: maximum 2, title as Heading 3, calendar icon, date and time formats from Configure are applied', async ({ page }) => {
    const url = await configureHost(page, 'calendar-2-0-list-options', async f => {
      await sel(f, 'perc_layout_type', 'list');
      await txt(f, 'perc_max_results', '2');
      await sel(f, 'perc_event_title_tag_type', 'h3');
      await chk(f, 'enableCalenderIcon', true);
      await chk(f, 'showEventDate', true);
      await txt(f, 'eventDateFormat', 'YYYY-MM-DD');
      await chk(f, 'showEventTime', true);
      await txt(f, 'eventTimeFormat', 'HH:mm');
    });
    await page.goto(url);
    const rows = page.locator('.perc-event-list li.perc-event-list-element');
    await expect(rows, 'maximum of 2 events').toHaveCount(2, { timeout: 60_000 });
    await expect(rows.first().locator('h3 .perc-event-title-link'), 'title is a Heading 3').toHaveText(events[0].title);
    await expect(rows.first().locator('.perc-cal-icon'), 'calendar icon').toBeVisible();
    const d = new Date(); d.setDate(d.getDate() + 1);
    await expect(rows.first().locator('.perc-date-dd'), 'calendar icon day').toHaveText(String(d.getDate()));
    await expect(rows.first().locator('.perc-event-date'), 'date format').toHaveText(`${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`);
    await expect(rows.first().locator('.perc-event-time'), 'time format').toContainText(/^09:00/);
    await page.screenshot({ path: 'screenshots/calendar-2-0-published-list-two-events-h3-icon-formats.png' });
  });

  test('Calendar 2.0 Event List: show event date and show event time off, calendar icon off remove them', async ({ page }) => {
    const url = await configureHost(page, 'calendar-2-0-list-hide', async f => {
      await sel(f, 'perc_layout_type', 'list');
      await txt(f, 'perc_max_results', '5');
      await sel(f, 'perc_event_title_tag_type', 'p');
      await chk(f, 'enableCalenderIcon', false);
      await chk(f, 'showEventDate', false);
      await chk(f, 'showEventTime', false);
    });
    await page.goto(url);
    const rows = page.locator('.perc-event-list li.perc-event-list-element');
    await expect(rows, 'all three events').toHaveCount(3, { timeout: 60_000 });
    await expect(page.locator('.perc-event-list .perc-event-date'), 'no date').toHaveCount(0);
    await expect(page.locator('.perc-event-list .perc-event-time'), 'no time').toHaveCount(0);
    await expect(page.locator('.perc-event-list .perc-cal-icon'), 'no calendar icon').toHaveCount(0);
    await expect(rows.first().locator('p .perc-event-title-link'), 'title is a paragraph').toHaveText(events[0].title);
    await page.screenshot({ path: 'screenshots/calendar-2-0-published-list-date-time-icon-hidden.png' });
  });

  test('Calendar 2.0 Calendar: agenda week view, week start day, French button texts and 24 hour event time are applied', async ({ page }) => {
    const tomorrow = new Date(); tomorrow.setDate(tomorrow.getDate() + 1);
    const url = await configureHost(page, 'calendar-2-0-calendar-week-agenda-fr', async f => {
      await sel(f, 'perc_layout_type', 'calendar');
      await sel(f, 'perc_calendar_type', 'Week');
      await sel(f, 'perc_calendar_detail', 'agenda');
      await sel(f, 'perc_start_day', `Day_${tomorrow.getDay()}`);
      await sel(f, 'perc_time_format', 'military');
      await sel(f, 'perc_calendar_display_language', 'fr');
    });
    await page.goto(url);
    await expect(page.locator('.fc-agendaWeek-view'), 'agenda week view').toBeVisible({ timeout: 60_000 });
    const frDay = tomorrow.toLocaleString('fr', { weekday: 'short' }).replace('.', '').slice(0, 3);
    await expect(page.locator('.fc-day-header').first(), 'the week starts with the chosen day (tomorrow), named in French').toContainText(new RegExp(frDay, 'i'));
    await expect(page.locator('.fc-month-button'), 'button text in French').toHaveText(/mois/i);
    await expect(page.locator('.fc-today-button'), 'Today button in French').toHaveText(/aujourd/i);
    await page.locator('.fc-next-button').click();   // today is in the first week (it starts before tomorrow): the events are in the next one
    for (const ev of events) await expect(page.locator('.fc-event', { hasText: ev.title }), `${ev.title} in the week`).toBeVisible();
    const times = await page.locator('.fc-event .fc-time').allInnerTexts();
    expect(times.length, 'a time on each event').toBe(3);
    for (const t of times) expect(t, '24 hour time range without am/pm').toMatch(/^\d{2}:\d{2} - \d{2}:\d{2}$/);
    expect(times.join(' '), 'the 09:00 event').toContain('09:00 - 10:00');
    expect(times.join(' '), 'the 14:00 event is written as 14:00, not 2pm').toContain('14:00 - 15:00');
    await page.screenshot({ path: 'screenshots/calendar-2-0-published-calendar-week-agenda-french-24h.png', fullPage: true });
  });

  test('Calendar 2.0 Calendar: view toggle buttons switch between the calendar and the list, and the Percussion events button label is written', async ({ page }) => {
    const url = await configureHost(page, 'calendar-2-0-calendar-toggle', async f => {
      await sel(f, 'perc_layout_type', 'calendar');
      await sel(f, 'perc_calendar_type', 'month');
      await sel(f, 'perc_calendar_detail', 'basic');
      await sel(f, 'perc_start_day', 'Day_1');
      await sel(f, 'perc_time_format', 'ampm');
      await sel(f, 'perc_calendar_display_language', 'en');
      await chk(f, 'perc_enable_view_toggle_buttons', true);
      await sel(f, 'perc_show_perc_events_button', 'Y');
      await txt(f, 'perc_event_button_label', `Site events ${run}`);
    });
    await page.goto(url);
    await expect(page.locator('.fc-month-view'), 'month view').toBeVisible({ timeout: 60_000 });
    await expect(page.locator('.fc-day-header').first(), 'the week starts on Monday').toContainText(/mon/i);
    await expect(page.locator('#perc-calendar-other-button'), 'Percussion events button label').toHaveText(`Site events ${run}`);
    await expect(page.locator('.perc-toggle-button-container .perc-type-toggle'), 'two toggle buttons').toHaveCount(2);
    await page.getByRole('button', { name: 'List View' }).click();
    await expect(page.locator('.perc-event-list li.perc-event-list-element').first(), 'list view shows the events').toBeVisible({ timeout: 30_000 });
    await expect(page.locator('.fc-month-view'), 'calendar hidden in list view').toBeHidden();
    await page.getByRole('button', { name: 'Calendar View' }).click();
    await expect(page.locator('.fc-month-view'), 'calendar shown again').toBeVisible();
    await page.screenshot({ path: 'screenshots/calendar-2-0-published-calendar-toggle-buttons.png', fullPage: true });
  });

  test('Calendar 2.0 Calendar: the French language writes the month and day names in French', async ({ page }) => {
    const url = await configureHost(page, 'calendar-2-0-calendar-month-fr', async f => {
      await sel(f, 'perc_layout_type', 'calendar');
      await sel(f, 'perc_calendar_type', 'month');
      await sel(f, 'perc_calendar_detail', 'basic');
      await sel(f, 'perc_start_day', 'Day_1');
      await sel(f, 'perc_time_format', 'ampm');
      await sel(f, 'perc_calendar_display_language', 'fr');
      await chk(f, 'perc_enable_view_toggle_buttons', false);
      await sel(f, 'perc_show_perc_events_button', 'N');
    });
    await page.goto(url);
    await expect(page.locator('.fc-month-view'), 'month view').toBeVisible({ timeout: 60_000 });
    const month = new Date().toLocaleString('fr', { month: 'long' });
    await expect.soft(page.locator('.fc-toolbar h2, .fc-left h2').first(), 'month name in French').toContainText(new RegExp(month, 'i'));
    await expect.soft(page.locator('.fc-day-header').first(), 'Monday in French (lun)').toContainText(/lun/i);
    await page.screenshot({ path: 'screenshots/calendar-2-0-published-calendar-month-french.png', fullPage: true });
  });
});
