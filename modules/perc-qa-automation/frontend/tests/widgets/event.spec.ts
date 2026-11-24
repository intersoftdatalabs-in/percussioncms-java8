import { test, expect, Page, FrameLocator } from '@playwright/test';
import { loginAsAdmin, createPage, publishOpenItem, configurePublishing, reopenPageInEditor } from '../helpers/cms-ui';
import { placeWidget, saveLayout, openContentTab, openWidgetContentEditor, openWidgetConfigure, confirmWidgetConfigure } from '../helpers/widgets-ui';
import { DTS_URL, DTS_ADMIN_URL, TEST_FOLDER } from '../helpers/qa-env';

/**
 * Event widget: shows one event (title, start/end date and time, location, summary, description) on a page. It is rendered by the CMS
 * when the page is published, so the proof is the PUBLISHED page served by the DTS. The dates are chosen with the widget's own
 * date-and-time picker (day grid + hour slider + Done), not typed.
 */

/** Opens the picker behind `trigger` (0 = start, 1 = end), clicks the day `daysAhead` days from today, sets the hour with the slider keys and presses Done. */
async function pickDateTime(frame: FrameLocator, trigger: 0 | 1, daysAhead: number, hour: number) {
  const target = new Date();
  target.setDate(target.getDate() + daysAhead);
  await frame.locator('.ui-datepicker-trigger').nth(trigger).click();
  const picker = frame.locator('#ui-datepicker-div');
  await expect(picker).toBeVisible({ timeout: 10_000 });
  if (target.getMonth() !== new Date().getMonth()) await picker.locator('.ui-datepicker-next').click();
  await picker.locator('td:not(.ui-datepicker-other-month) a', { hasText: new RegExp(`^${target.getDate()}$`) }).click();
  const hourHandle = frame.locator('.ui-slider-handle').first();   // the time sliders are not inside #ui-datepicker-div itself
  await hourHandle.focus({ timeout: 10_000 });
  for (let i = 0; i < hour; i++) await hourHandle.press('ArrowRight');
  await picker.getByRole('button', { name: 'Done' }).click();
  await expect(picker).toBeHidden({ timeout: 10_000 });
}

const pad = (n: number) => String(n).padStart(2, '0');
const inDays = (days: number) => { const d = new Date(); d.setDate(d.getDate() + days); return d; };
const iso = (d: Date) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;

/** Fills the Event content editor, saves it and publishes the page. */
async function fillEventAndPublish(page: Page, ev: { title: string; location?: string; summary?: string; description?: string }, shot: string) {
  const { frame, save } = await openWidgetContentEditor(page, 'percEvent');
  await frame.locator('#perc-content-edit-displaytitle').pressSequentially(ev.title, { delay: 10 });
  await pickDateTime(frame, 0, 3, 9);
  await pickDateTime(frame, 1, 4, 17);
  if (ev.location) await frame.locator('#perc-content-edit-location').pressSequentially(ev.location, { delay: 10 });
  if (ev.summary) await frame.locator('#callout').pressSequentially(ev.summary, { delay: 10 });
  if (ev.description) {
    const body = frame.frameLocator('#ce_bodybody_ifr').locator('body');
    await body.click();
    await body.pressSequentially(ev.description, { delay: 10 });
  }
  await expect(frame.locator('#perc-content-edit-start_time-display'), 'start chosen with the picker').toHaveValue(`${iso(inDays(3))} 09:00 am`);
  await expect(frame.locator('#perc-content-edit-end_time-display'), 'end chosen with the picker').toHaveValue(`${iso(inDays(4))} 05:00 pm`);
  await page.screenshot({ path: `screenshots/${shot}-content-editor-filled.png` });
  await save.click();
  await expect(save).toBeHidden({ timeout: 30_000 });
  await openContentTab(page);
  await publishOpenItem(page);
}

/** Opens the page again in the editor, sets Configure options, saves and publishes. */
async function reconfigureAndPublish(page: Page, host: string, editUrl: string, shot: string, set: (cfg: ReturnType<Page['locator']>) => Promise<void>) {
  await reopenPageInEditor(page, editUrl, host);   // the finder lists only the first 100 pages of the widgets folder: reopen by the editor URL
  const cfg = await openWidgetConfigure(page, 'percEvent');
  await set(cfg);
  await page.screenshot({ path: `screenshots/${shot}-configure-dialog.png` });
  await confirmWidgetConfigure(page);
  await saveLayout(page);
  await openContentTab(page);
  await publishOpenItem(page);
}

test.describe('Event widget', () => {
  test.setTimeout(420_000);

  test('Event widget: published page shows the title, start and end date and time, location, summary and description', async ({ page }) => {
    const run = Date.now().toString().slice(-7);
    const ev = { title: `QA Event ${run}`, location: `QA Hall ${run}`, summary: `Summary of event ${run}.`, description: `Description of event ${run}.` };
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const host = await createPage(page, `Event Host ${run}`);
    await placeWidget(page, 'percEvent');
    await saveLayout(page);
    await fillEventAndPublish(page, ev, 'event-widget-all-fields');
    const editUrl = page.url();

    await page.goto(`${DTS_URL}/${TEST_FOLDER}/${host}.html`);
    const root = page.locator('.perc-event');
    await expect(root.locator('.perc-event-title')).toHaveText(ev.title);
    await expect(root.locator('.summary')).toHaveText(ev.summary);
    await expect(root.locator('.description')).toContainText(ev.description);
    await expect(root.locator('.location')).toHaveText(ev.location);
    const s = inDays(3), e = inDays(4);
    await expect(root.locator('abbr.dtstart'), 'start date and time in the default format').toHaveText(new RegExp(`^${s.getMonth() + 1}/${s.getDate()}/${s.getFullYear()} 09:00 (AM|am)$`));
    await expect(root.locator('abbr.dtend'), 'end date and time in the default format').toHaveText(new RegExp(`^${e.getMonth() + 1}/${e.getDate()}/${e.getFullYear()} 05:00 (PM|pm)$`));
    await expect(root.locator('abbr.dtstart')).toHaveAttribute('title', new RegExp(`^${iso(s)}`));
    await page.screenshot({ path: 'screenshots/event-widget-published-page-shows-all-event-fields.png' });
  });

  test('Event widget: hidden fields are left out of the published page and the start date format from Configure is used', async ({ page }) => {
    const run = Date.now().toString().slice(-7);
    const ev = { title: `QA Event Config ${run}`, location: `QA Hidden Hall ${run}`, summary: `Hidden summary ${run}.`, description: `Description of config event ${run}.` };
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const host = await createPage(page, `Event Config Host ${run}`);
    await placeWidget(page, 'percEvent');
    await saveLayout(page);
    // every field is filled in and published first, so that a missing field on the page can only come from the Configure options
    await fillEventAndPublish(page, ev, 'event-widget-hidden-fields');
    const editUrl = page.url();
    await page.goto(`${DTS_URL}/${TEST_FOLDER}/${host}.html`);
    await expect(page.locator('.perc-event .location'), 'location shown before it is hidden').toHaveText(ev.location);
    await expect(page.locator('.perc-event .summary'), 'summary shown before it is hidden').toHaveText(ev.summary);

    await reopenPageInEditor(page, editUrl, host);
    const cfg = await openWidgetConfigure(page, 'percEvent');
    await cfg.locator('input[name="perc_hidefield_location"]').check();
    await cfg.locator('input[name="perc_hidefield_callout"]').check();
    await cfg.locator('input[name="startDateTimeFormat"]').fill('EEEE, MMMM d, yyyy');
    await page.screenshot({ path: 'screenshots/event-widget-hidden-fields-configure-dialog.png' });
    await confirmWidgetConfigure(page);
    await saveLayout(page);
    await openContentTab(page);
    await publishOpenItem(page);

    await page.goto(`${DTS_URL}/${TEST_FOLDER}/${host}.html`);
    const root = page.locator('.perc-event');
    await expect(root.locator('.perc-event-title')).toHaveText(ev.title);
    await expect(root.locator('.description')).toContainText(ev.description);
    await expect(root.locator('.location'), 'location is hidden').toHaveCount(0);
    await expect(root.locator('.summary'), 'summary is hidden').toHaveCount(0);
    const long = inDays(3).toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' });
    await expect(root.locator('abbr.dtstart'), 'start date in the format set in Configure').toHaveText(long);
    await page.screenshot({ path: 'screenshots/event-widget-published-page-hides-location-and-summary.png' });
  });

  test('Event widget: hiding the title, the description and the start date, and the end date format from Configure are applied', async ({ page }) => {
    const run = Date.now().toString().slice(-7);
    const ev = { title: `QA Event Hide ${run}`, location: `QA Hide Hall ${run}`, summary: `Hide summary ${run}.`, description: `Hide description ${run}.` };
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const host = await createPage(page, `Event Hide Host ${run}`);
    await placeWidget(page, 'percEvent');
    await saveLayout(page);
    await fillEventAndPublish(page, ev, 'event-widget-hide-title-description-start');
    const editUrl = page.url();
    await reconfigureAndPublish(page, host, editUrl, 'event-widget-hide-title-description-start-end-format', async cfg => {
      await cfg.locator('input[name="perc_hidefield_displaytitle"]').check();
      await cfg.locator('input[name="perc_hidefield_body"]').check();
      await cfg.locator('input[name="perc_hidefield_start_time"]').check();
      await cfg.locator('input[name="endDateTimeFormat"]').fill('yyyy/MM/dd HH:mm');
    });
    await page.goto(`${DTS_URL}/${TEST_FOLDER}/${host}.html`);
    const root = page.locator('.perc-event');
    await expect(root.locator('.location'), 'location is still shown').toHaveText(ev.location);
    await expect(root.locator('.perc-event-title'), 'title is hidden').toHaveCount(0);
    await expect(root.locator('.description'), 'description is hidden').toHaveCount(0);
    await expect(root.locator('abbr.dtstart'), 'start date is hidden').toHaveCount(0);
    const e = inDays(4);
    await expect(root.locator('abbr.dtend'), 'end date in the format set in Configure').toHaveText(`${e.getFullYear()}/${pad(e.getMonth() + 1)}/${pad(e.getDate())} 17:00`);
    await page.screenshot({ path: 'screenshots/event-widget-published-page-title-description-start-hidden-end-format.png' });
  });

  test('Event widget: hiding the end date removes it, and "Render empty fields" writes the empty location, summary and description', async ({ page }) => {
    const run = Date.now().toString().slice(-7);
    const ev = { title: `QA Event Empty ${run}` };   // only the title and the dates: location, summary and description stay empty
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const host = await createPage(page, `Event Empty Host ${run}`);
    await placeWidget(page, 'percEvent');
    await saveLayout(page);
    await fillEventAndPublish(page, ev, 'event-widget-empty-fields');
    const editUrl = page.url();
    const url = `${DTS_URL}/${TEST_FOLDER}/${host}.html`;
    await page.goto(url);
    const root = page.locator('.perc-event');
    await expect(root.locator('.perc-event-title'), 'the event is shown').toHaveText(ev.title);
    for (const c of ['.location', '.summary', '.description']) await expect(root.locator(c), `empty ${c} is not written by default`).toHaveCount(0);
    await expect(root.locator('abbr.dtend'), 'end date is shown by default').toHaveCount(1);

    await reconfigureAndPublish(page, host, editUrl, 'event-widget-render-empty-fields-and-hide-end', async cfg => {
      await cfg.locator('input[name="renderEmptyFields"]').check();
      await cfg.locator('input[name="perc_hidefield_end_time"]').check();
    });
    await page.goto(url);
    for (const c of ['.location', '.summary', '.description']) {
      await expect(root.locator(c), `empty ${c} is written when Render empty fields is on`).toHaveCount(1);
      await expect(root.locator(c)).toHaveText('');
    }
    await expect(root.locator('abbr.dtend'), 'end date is hidden').toHaveCount(0);
    await expect(root.locator('abbr.dtstart'), 'start date is still shown').toHaveCount(1);
    await page.screenshot({ path: 'screenshots/event-widget-published-page-empty-fields-rendered-end-hidden.png' });
  });
});
