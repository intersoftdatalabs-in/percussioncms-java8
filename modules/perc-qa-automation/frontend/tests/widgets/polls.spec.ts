import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';
import { test, expect, Page, FrameLocator } from '@playwright/test';
import { loginAsAdmin, createPage, publishOpenItem, configurePublishing, ensureDeliveryResources } from '../helpers/cms-ui';
import { placeWidget, saveLayout, openContentTab, openWidgetContentEditor, openWidgetConfigure, confirmWidgetConfigure } from '../helpers/widgets-ui';
import { DTS_URL, DTS_ADMIN_URL, TEST_FOLDER } from '../helpers/qa-env';

/**
 * Polls: a question with answers (one choice or several) that visitors vote on. The votes are stored by the DTS polls service, so the proof is the
 * PUBLISHED page served by the DTS: the question and answers, the validation text when nothing is chosen, the results (percent, count, total) after
 * a vote, the labels chosen in Configure, the "view results" link, and what each restriction (Unrestricted, Session, Cookie) allows.
 */
process.env.QA_POLLS_RUN ??= Date.now().toString().slice(-7);   // shared with the workers Playwright restarts after a failed scenario
const run = process.env.QA_POLLS_RUN;

interface PollOptions { question: string; multi?: boolean; answers: string[]; title: string; }

/** Puts a Polls widget on a new page, configures it (when `configure` is given), fills in the poll and publishes. Returns the published URL. */
async function publishPollPage(page: Page, shot: string, poll: PollOptions, configure?: (form: ReturnType<Page['locator']>) => Promise<void>): Promise<string> {
  const host = await createPage(page, `Poll Host ${shot.slice(-7)} ${run}`);
  await placeWidget(page, 'percPoll');
  if (configure) {
    const form = await openWidgetConfigure(page, 'percPoll');
    await configure(form);
    await page.screenshot({ path: `screenshots/${shot}-configure-dialog.png` });
    await confirmWidgetConfigure(page);
  }
  await saveLayout(page);
  const { frame, save } = await openWidgetContentEditor(page, 'percPoll');
  await frame.locator('#perc-content-edit-pollTitle').pressSequentially(poll.title, { delay: 10 });
  await frame.locator('#perc-poll-question').fill(poll.question);
  await frame.locator(`#perc-poll-answer-type-${poll.multi ? 'multi' : 'single'}`).check();
  await fillAnswers(frame, poll.answers);
  await page.screenshot({ path: `screenshots/${shot}-content-editor-poll.png` });
  await save.click();
  await expect(save).toBeHidden({ timeout: 60_000 });
  await openContentTab(page);
  await publishOpenItem(page);
  return `${DTS_URL}/${TEST_FOLDER}/${host}.html`;
}

/** The editor starts with three answers; adds rows with the plus button until there are enough, then types the answers (extra rows stay empty and are dropped). */
async function fillAnswers(frame: FrameLocator, answers: string[]) {
  const boxes = frame.locator('.perc-answer-wrapper input.option-dd');
  while ((await boxes.count()) < answers.length) {
    await frame.locator('.perc-answer-wrapper .add-control').click();
  }
  for (let i = 0; i < (await boxes.count()); i++) await boxes.nth(i).fill(answers[i] ?? '');
}

const poll = (page: Page) => page.locator('.perc-polls');
const form = (page: Page) => poll(page).locator('.perc-poll-render-container');
const results = (page: Page) => poll(page).locator('.perc-poll-results-container');
const answerRow = (page: Page, text: string) => results(page).locator('.perc-poll-answer-container', { has: page.locator('.perc-poll-answer', { hasText: new RegExp(`^"?${text}"?$`) }) });

async function openPoll(page: Page, url: string, expectForm = true) {
  await page.goto(url);
  await expect(poll(page), 'the poll is on the published page').toBeAttached({ timeout: 60_000 });
  if (expectForm) await expect(form(page), 'the poll form is shown').toBeVisible({ timeout: 60_000 });
}

test.describe('Polls', () => {
  test.setTimeout(480_000);
  const single: PollOptions = { title: `QA Poll ${run}`, question: `Best colour ${run}?`, answers: [`Red ${run}`, `Green ${run}`, `Blue ${run}`] };
  // a failing scenario makes Playwright start a new worker, so the shared poll address is kept in a file, not only in memory
  const urlFile = path.join(os.tmpdir(), `qa-polls-single-${run}.txt`);
  let singleUrl = '';
  test.beforeEach(() => { if (!singleUrl && fs.existsSync(urlFile)) singleUrl = fs.readFileSync(urlFile, 'utf8'); });

  test('Polls: published page shows the question and answers as radio buttons with the default labels, and asks for an answer when none is chosen', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    await ensureDeliveryResources(page);
    singleUrl = await publishPollPage(page, 'polls-single-defaults', single);
    fs.writeFileSync(urlFile, singleUrl);

    await openPoll(page, singleUrl);
    await expect(form(page).locator('.perc-poll-title'), 'poll title').toHaveText(single.title);
    await expect(form(page).locator('.perc-poll-question'), 'question').toHaveText(single.question);
    await expect(form(page).locator('input[name="perc-poll-answer"][type="radio"]'), 'one radio button per answer').toHaveCount(3);
    await expect(form(page).locator('.perc-poll-answer'), 'answers (quotation marks aside, see the last scenario)').toHaveText(single.answers.map(a => new RegExp('^"?' + a + '"?$')));
    await expect(form(page).locator('input[name="perc-poll-submit-button"]'), 'default submit label').toHaveValue('Vote');
    await expect(form(page).locator('.view-results-link a'), 'default view results text').toHaveText('View results');
    await form(page).locator('.perc-poll-submit-button-span-action').click();
    await expect(form(page).locator('.poll-validation-text'), 'default validation text').toHaveText('Please choose an answer first!');
    await expect(form(page).locator('.poll-validation-text')).toBeVisible();
    await form(page).locator('input[name="perc-poll-answer"]').first().click();
    await expect(form(page).locator('.poll-validation-text'), 'message goes away once an answer is chosen').toBeHidden();
    await page.screenshot({ path: 'screenshots/polls-published-page-single-choice-form.png' });
  });

  test('Polls: a vote shows the results with percent, count and total, "View results" shows them to another visitor, and "Back to poll" returns to the form', async ({ page, browser }) => {
    expect(singleUrl, 'page from the first scenario').not.toBe('');
    await openPoll(page, singleUrl);
    await form(page).locator('.perc-poll-answer-container', { hasText: single.answers[0] }).locator('input').check();
    await form(page).locator('.perc-poll-submit-button-span-action').click();
    await expect(results(page), 'results are shown after the vote').toBeVisible({ timeout: 30_000 });
    await expect(form(page)).toBeHidden();
    await expect(answerRow(page, single.answers[0]).locator('.perc-poll-result-percent'), 'all votes on the first answer').toHaveText('100%');
    await expect(answerRow(page, single.answers[0]).locator('.perc-poll-result-count')).toHaveText('(1/1)');
    await expect(results(page).locator('.perc-poll-total-votes'), 'default total votes text').toHaveText(/^Total votes:\s*1$/);
    await expect(results(page).locator('.perc-poll-backto-poll a'), 'default back text').toHaveText('Back to poll');
    await page.screenshot({ path: 'screenshots/polls-published-page-results-after-first-vote.png' });

    // a second visitor (no cookie) opens "View results" without voting, then votes for another answer
    const other = await browser.newContext();
    try {
      const b = await other.newPage();
      await openPoll(b, singleUrl);
      await form(b).locator('.view-results-link a').click();
      await expect(results(b)).toBeVisible({ timeout: 30_000 });
      await expect(results(b).locator('.perc-poll-total-votes'), 'sees the first visitor\'s vote').toHaveText(/^Total votes:\s*1$/);
      await results(b).locator('.perc-poll-backto-poll a').click();
      await expect(form(b), 'back to poll shows the form again').toBeVisible();
      await form(b).locator('.perc-poll-answer-container', { hasText: single.answers[1] }).locator('input').check();
      await form(b).locator('.perc-poll-submit-button-span-action').click();
      await expect(results(b).locator('.perc-poll-total-votes')).toHaveText(/^Total votes:\s*2$/, { timeout: 30_000 });
      await expect(answerRow(b, single.answers[0]).locator('.perc-poll-result-percent'), 'one of two votes').toHaveText('50%');
      await expect(answerRow(b, single.answers[1]).locator('.perc-poll-result-count')).toHaveText('(1/2)');
      await b.screenshot({ path: 'screenshots/polls-published-page-results-after-second-vote.png' });
    } finally { await other.close(); }
  });

  test('Polls: a multiple-selection poll shows check boxes and counts every chosen answer', async ({ page }) => {
    const multi: PollOptions = { title: `QA Multi ${run}`, multi: true, question: `Which fruit ${run}?`, answers: [`Apple ${run}`, `Banana ${run}`, `Cherry ${run}`, `Date ${run}`] };
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishPollPage(page, 'polls-multiple-selection', multi);
    await openPoll(page, url);
    await expect(form(page).locator('input[name="perc-poll-answer"][type="checkbox"]'), 'a check box per answer (four, one added in the editor)').toHaveCount(4);
    await form(page).locator('.perc-poll-answer-container', { hasText: multi.answers[0] }).locator('input').check();
    await form(page).locator('.perc-poll-answer-container', { hasText: multi.answers[2] }).locator('input').check();
    await form(page).locator('.perc-poll-submit-button-span-action').click();
    await expect(results(page)).toBeVisible({ timeout: 30_000 });
    await expect(answerRow(page, multi.answers[0]).locator('.perc-poll-result-count'), 'first chosen answer counted (the total counts selections, so two chosen answers make a total of 2)').toHaveText('(1/2)');
    await expect(answerRow(page, multi.answers[2]).locator('.perc-poll-result-count'), 'second chosen answer counted').toHaveText('(1/2)');
    await page.screenshot({ path: 'screenshots/polls-published-page-multiple-selection-results.png' });
  });

  test('Polls: the submit label, view results text, validation text, total votes text and return text from Configure are used', async ({ page }) => {
    const p: PollOptions = { title: `QA Labels ${run}`, question: `Labels question ${run}?`, answers: [`One ${run}`, `Two ${run}`] };
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishPollPage(page, 'polls-custom-labels', p, async f => {
      await f.locator('input[name="submitLabel"]').fill('Cast vote');
      await f.locator('input[name="viewResultsLinkText"]').fill('See the tally');
      await f.locator('input[name="validationText"]').fill('Pick one please');
      await f.locator('input[name="totalVotesText"]').fill('Votes so far');
      await f.locator('input[name="backToPollText"]').fill('Return to the poll');
    });
    await openPoll(page, url);
    await expect(form(page).locator('input[name="perc-poll-submit-button"]'), 'submit label').toHaveValue('Cast vote');
    await expect(form(page).locator('.view-results-link a'), 'view results text').toHaveText('See the tally');
    await form(page).locator('.perc-poll-submit-button-span-action').click();
    await expect(form(page).locator('.poll-validation-text'), 'validation text').toHaveText('Pick one please');
    await expect(form(page).locator('.poll-validation-text')).toBeVisible();
    await form(page).locator('.perc-poll-answer-container', { hasText: p.answers[0] }).locator('input').check();
    await form(page).locator('.perc-poll-submit-button-span-action').click();
    await expect(results(page)).toBeVisible({ timeout: 30_000 });
    await expect(results(page).locator('.perc-poll-total-votes'), 'total votes text').toHaveText(/^Votes so far:\s*1$/);
    await expect(results(page).locator('.perc-poll-backto-poll a'), 'return text').toHaveText('Return to the poll');
    await page.screenshot({ path: 'screenshots/polls-published-page-custom-labels.png' });
  });

  test('Polls: "Show view results link" off removes the link', async ({ page }) => {
    const p: PollOptions = { title: `QA NoLink ${run}`, question: `No link question ${run}?`, answers: [`Yes ${run}`, `No ${run}`] };
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishPollPage(page, 'polls-no-results-link', p, async f => {
      await f.locator('input[name="viewResultsLink"]').uncheck();
    });
    await openPoll(page, url);
    await expect(form(page).locator('.view-results-link a'), 'no view results link').toHaveCount(0);
    await expect(form(page).locator('.perc-poll-submit-button-span-action'), 'the poll can still be voted on').toBeVisible();
    await page.screenshot({ path: 'screenshots/polls-published-page-without-view-results-link.png' });
  });

  test('Polls: restriction "Cookie" shows the results instead of the form to a visitor who already voted, with no way back to the poll', async ({ page }) => {
    const p: PollOptions = { title: `QA Cookie ${run}`, question: `Cookie question ${run}?`, answers: [`A ${run}`, `B ${run}`] };
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishPollPage(page, 'polls-restrict-cookie', p, async f => {
      await f.locator('select[name="pollRestrictionType"]').selectOption('Cookie', { timeout: 10_000 });
    });
    await openPoll(page, url);
    await form(page).locator('.perc-poll-answer-container', { hasText: p.answers[0] }).locator('input').check();
    await form(page).locator('.perc-poll-submit-button-span-action').click();
    await expect(results(page)).toBeVisible({ timeout: 30_000 });
    await expect(results(page).locator('.perc-poll-backto-poll'), 'no way back to the form after voting').toBeHidden();
    await page.reload();
    await expect(results(page), 'a returning visitor who voted sees the results').toBeVisible({ timeout: 60_000 });
    await expect(form(page), 'and not the form').toBeHidden();
    await expect(results(page).locator('.perc-poll-total-votes')).toHaveText(/^Total votes:\s*1$/);
    await page.screenshot({ path: 'screenshots/polls-published-page-restricted-by-cookie-after-vote.png' });
  });

  test('Polls: the answers are shown as typed, without quotation marks around them', async ({ page }) => {
    expect(singleUrl, 'page from the first scenario').not.toBe('');
    await openPoll(page, singleUrl);
    await expect(form(page).locator('.perc-poll-answer'), 'answer texts exactly as typed').toHaveText(single.answers);
  });

  test('Polls: an answer nobody voted for shows 0% and a count of 0 in the results', async ({ page }) => {
    expect(singleUrl, 'page from the first scenario').not.toBe('');
    await openPoll(page, singleUrl);
    await form(page).locator('.view-results-link a').click();
    await expect(results(page)).toBeVisible({ timeout: 30_000 });
    await expect(answerRow(page, single.answers[2]).locator('.perc-poll-result-percent'), 'no votes: 0%').toHaveText('0%');
    await expect(answerRow(page, single.answers[2]).locator('.perc-poll-result-count'), 'no votes: (0/2)').toHaveText('(0/2)');
  });

  test('Polls: restriction "Session" shows the results instead of the form to a visitor who already voted in this session', async ({ page }) => {
    const p: PollOptions = { title: `QA Session ${run}`, question: `Session question ${run}?`, answers: [`A ${run}`, `B ${run}`] };
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishPollPage(page, 'polls-restrict-session', p, async f => {
      await f.locator('select[name="pollRestrictionType"]').selectOption('Session', { timeout: 10_000 });
    });
    await openPoll(page, url);
    await form(page).locator('.perc-poll-answer-container', { hasText: p.answers[1] }).locator('input').check();
    await form(page).locator('.perc-poll-submit-button-span-action').click();
    await expect(results(page)).toBeVisible({ timeout: 30_000 });
    await expect(results(page).locator('.perc-poll-backto-poll'), 'no way back to the form after voting').toBeHidden();
    await page.reload();
    await expect(results(page), 'the same session sees the results').toBeVisible({ timeout: 60_000 });
    await expect(form(page)).toBeHidden();
    await expect(answerRow(page, p.answers[1]).locator('.perc-poll-result-percent')).toHaveText('100%');
    await page.screenshot({ path: 'screenshots/polls-published-page-restricted-by-session-after-vote.png' });
  });
});
