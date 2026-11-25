import * as path from 'path';
import { test, expect, Page } from '@playwright/test';
import { loginAsAdmin, createPage, publishOpenItem, configurePublishing, ensureDeliveryResources, uploadImageAsset } from '../helpers/cms-ui';
import { chooseImageFromLibrary } from '../helpers/asset-chooser';
import { placeWidget, saveLayout, openContentTab, openWidgetConfigure, confirmWidgetConfigure } from '../helpers/widgets-ui';
import { createAssetIn, dragAssetOntoWidget, takeAssetToLive, choosePageFromTree } from '../helpers/directory-ui';
import { DTS_URL, DTS_ADMIN_URL, TEST_FOLDER, TEST_ASSET_FOLDER } from '../helpers/qa-env';

/**
 * Directory: lists the Person assets of an Organization (or of all organizations), optionally of one Department, as a table or as cards, with a text search,
 * a filter by the first letter of the last name, sorting and (for all organizations) a filter by organization. The proof is the PUBLISHED page served by the
 * DTS. The assets are created once, in a folder tree of their own:
 *   Assets / qa-assets / qa-dir-<run> / org-a / (Org A) / dept-a / (Dept A, Adams, Brown) , dept-b / (Dept B, Clark)
 *                                     / org-b / (Org B) / dept-c / (Dept C, Dunn)
 */
process.env.QA_DIRW_RUN ??= Date.now().toString().slice(-7);   // shared with the workers Playwright restarts after a failed scenario
const run = process.env.QA_DIRW_RUN;
const IMAGE = path.join(__dirname, '..', 'fixtures', 'qa-image.png');   // 160 x 100 PNG
const base = [TEST_ASSET_FOLDER, `qa-dir-${run}`];
const orgA = `Dir Org A ${run}`, orgB = `Dir Org B ${run}`;
const dptA = `Dir Dept A ${run}`, dptB = `Dir Dept B ${run}`, dptC = `Dir Dept C ${run}`;
const people = [
  { first: 'Alice', last: `Adams${run}`, title: 'Analyst', phone: '555-0401', office: 'Room 1', folders: [...base, 'org-a', 'dept-a'], org: orgA, dpt: dptA },
  { first: 'Brian', last: `Brown${run}`, title: 'Builder', phone: '555-0402', office: 'Room 2', folders: [...base, 'org-a', 'dept-a'], org: orgA, dpt: dptA },
  { first: 'Carla', last: `Clark${run}`, title: 'Clerk', phone: '555-0403', office: 'Room 3', folders: [...base, 'org-a', 'dept-b'], org: orgA, dpt: dptB },
  { first: 'Dana', last: `Dunn${run}`, title: 'Driver', phone: '555-0404', office: 'Room 4', folders: [...base, 'org-b', 'dept-c'], org: orgB, dpt: dptC },
];
const assets = { all: `Directory All ${run}`, orgA: `Directory Org A ${run}`, dptB: `Directory Dept B ${run}` };
const assetNames: Record<string, string> = {};
const imageTitle = `QA Dir Photo ${run}`;
let mailForm = `aa-dir-mail-form-${run}`;   // the file name the CMS gives the page (from its title); set again by the first scenario
// QA_DIRW_REUSE=true (with QA_DIRW_RUN of an earlier run): the library of that run is used as it is, the first scenario is skipped
const reuse = process.env.QA_DIRW_REUSE === 'true';
if (reuse) for (const k of Object.keys(assets) as (keyof typeof assets)[]) assetNames[k] = assets[k].toLowerCase().replace(/\s+/g, '-');

/** A new page with a Directory widget (optionally configured), the shared Directory asset dropped on it, published. Returns the published URL. */
async function publishDirectoryPage(page: Page, shot: string, which: keyof typeof assets | 'placeholder', configure?: (form: ReturnType<Page['locator']>) => Promise<void>): Promise<string> {
  await ensureDeliveryResources(page);   // the script of the directory is a delivery resource: a redeploy of the DTS removes it
  const host = await createPage(page, `Directory Host ${shot.replace(/^dir-/, '')} ${run} ${Date.now().toString().slice(-4)}`);
  await placeWidget(page, 'percDirectory');
  if (configure) {
    const form = await openWidgetConfigure(page, 'percDirectory');
    await configure(form);
    await page.screenshot({ path: `screenshots/${shot}-configure-dialog.png` });
    await confirmWidgetConfigure(page);
  }
  await saveLayout(page);
  await openContentTab(page);
  await dragAssetOntoWidget(page, 'percDirectory', base, assetNames[which]);
  await page.screenshot({ path: `screenshots/${shot}-directory-on-page.png` });
  await publishOpenItem(page);
  return `${DTS_URL}/${TEST_FOLDER}/${host}.html`;
}

const list = (page: Page) => page.locator('.percDirectory');
const rows = (page: Page) => list(page).locator('.perc-person').filter({ hasText: run });   // only the people of this run: the library holds the people of the other specs too
const lastNames = async (page: Page) => (await rows(page).locator('.perc-person-last-name').allTextContents()).map(t => t.trim()).sort();

/** Opens the published directory and removes the letter filter, which the script turns on for the first letter. */
async function openDirectory(page: Page, url: string, clearFilter = true) {
  await page.goto(url);
  await expect(list(page).locator('#percDirectoryList'), 'the directory is on the page').toBeAttached({ timeout: 60_000 });
  await expect(rows(page).first(), 'the people are listed').toBeVisible({ timeout: 30_000 });
  if (clearFilter && await page.locator('#perc-clear-alpha-filter').isVisible()) await page.locator('#perc-clear-alpha-filter').click();
}

/** Types in the search box with real key presses (the list script listens for key events). */
async function typeSearch(page: Page, text: string) {
  const box = page.locator('#search-directory');
  await box.click();
  await box.press('Control+a');
  await box.press('Backspace');
  if (text) await box.pressSequentially(text, { delay: 15 });
}

/** Saves the person asset that is open once more: the links to its organization and department are filled in when the editor opens. */
async function saveAgain(page: Page) {
  await page.getByRole('button', { name: 'Save' }).click();
  await page.waitForTimeout(4_000);
}

test.describe.serial('Directory', () => {
  test.setTimeout(360_000);

  test('Directory: all organizations, as a table with the default columns: the people are listed by last name with name, organization, department and position', async ({ page }) => {
    test.skip(reuse, 'the library of an earlier run is used');
    test.setTimeout(1_800_000);   // creates and publishes the whole library of the other scenarios
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    let image = '';
    for (let attempt = 1; !image; attempt++) {   // the upload form sometimes refuses the file ("Must select a valid image file"): try again
      try { image = await uploadImageAsset(page, TEST_ASSET_FOLDER, IMAGE, imageTitle, `QA dir photo alt ${run}`); } catch (e) { if (attempt >= 3) throw e; }
    }
    await publishOpenItem(page);
    // the library: two organizations with their departments, past Draft because the DTS pages are assembled from public assets
    for (const [folders, orgName] of [[[...base, 'org-a'], orgA], [[...base, 'org-b'], orgB]] as [string[], string][]) {
      const a = await createAssetIn(page, folders, 'Organization', 'psx_cepercOrganization', { orgName });
      await takeAssetToLive(page, folders, a);
    }
    for (const [folders, dptName, orgName] of [[[...base, 'org-a', 'dept-a'], dptA, orgA], [[...base, 'org-a', 'dept-b'], dptB, orgA], [[...base, 'org-b', 'dept-c'], dptC, orgB]] as [string[], string, string][]) {
      const a = await createAssetIn(page, folders, 'Department', 'psx_cepercDepartment', { dptName });
      await expect(page.frameLocator('iframe[src*="psx_cepercDepartment"]').locator('#perc-content-display-dptOrganization'), `${dptName}: the organization is found through the folders`).toHaveValue(orgName, { timeout: 30_000 });
      await saveAgain(page);
      await takeAssetToLive(page, folders, a);
    }
    mailForm = await createPage(page, `AA Dir Mail Form ${run}`);
    await openContentTab(page);
    await publishOpenItem(page);
    for (const p of people) {
      const personAsset = await createAssetIn(page, p.folders, 'Person', 'psx_cepercPerson', {
        sys_title: `${p.first} ${p.last}`, personFirstName: p.first, personLastName: p.last, personTitle: p.title, personPhone: p.phone, personEmail: `${p.first.toLowerCase()}-${run}@example.com`, personOfficeLocation: p.office,
      }, async frame => {
        await choosePageFromTree(page, frame.getByRole('button', { name: 'Browse' }).nth(0), mailForm);
        if (p.first === 'Alice') await chooseImageFromLibrary(page, frame.getByRole('button', { name: 'Browse' }).nth(1), image);
      });
      const f = page.frameLocator('iframe[src*="psx_cepercPerson"]');
      await expect(f.locator('#perc-content-display-personOrganization'), `${p.last}: the organization is found through the folders`).toHaveValue(p.org, { timeout: 30_000 });
      await expect(f.locator('#perc-content-display-personDepartment'), `${p.last}: the department is found through the folders`).toHaveValue(p.dpt, { timeout: 30_000 });
      await saveAgain(page);
      await takeAssetToLive(page, p.folders, personAsset);   // the directory lists the people that are past Draft
    }
    // the three directories: all organizations, the first organization, the second department of the first organization
    for (const [key, orgLabel, dptLabel] of [['all', null, null], ['orgA', orgA, null], ['dptB', orgA, dptB]] as [keyof typeof assets, string | null, string | null][]) {
      assetNames[key] = await createAssetIn(page, base, 'Directory', 'psx_cepercDirectory', { directoryTitle: assets[key] }, async frame => {
        if (orgLabel) await frame.locator('select[name="organizationSearch"]').selectOption({ label: orgLabel });
        if (dptLabel) {   // the departments are listed when the organization is chosen: "<department> - <organization>"
          const opt = frame.locator('select[name="departmentID"] option', { hasText: dptLabel });
          await expect(opt).toHaveCount(1, { timeout: 15_000 });
          const val = (await opt.getAttribute('value'))!;
          await frame.locator('select[name="departmentID"]').selectOption(val);
          // directoryDropdown.js clears and reloads departmentID asynchronously on change; wait for it to restore the selection before Save
          await expect(frame.locator('select[name="departmentID"]')).toHaveValue(val, { timeout: 15_000 });
        }
      });
    }

    const url = await publishDirectoryPage(page, 'dir-defaults', 'all');
    await openDirectory(page, url);
    await expect(list(page).locator('h2.perc-directory-title'), 'directory title in a Heading 2').toHaveText(assets.all);
    await expect(rows(page), 'the four people').toHaveCount(4);
    expect(await lastNames(page), 'last names').toEqual(people.map(p => p.last).sort());
    const headers = (await list(page).locator('thead th').allTextContents()).map(t => t.trim()).filter(Boolean);
    expect(headers, 'default columns: phone, office location and e-mail are hidden').toEqual(['Name', 'Organization', 'Department', 'Title']);
    const adams = rows(page).filter({ hasText: people[0].last });
    await expect(adams.locator('.perc-person-first-name'), 'first name').toHaveText('Alice');
    await expect(adams.locator('.perc-person-org'), 'organization').toHaveText(orgA);
    await expect(adams.locator('.perc-person-dpt'), 'department').toHaveText(dptA);
    await expect(adams.locator('.perc-person-title'), 'position').toHaveText('Analyst');
    await expect(adams.locator('img'), 'the person has an image').toHaveAttribute('title', imageTitle);
    await expect(rows(page).filter({ hasText: people[1].last }).locator('img'), 'a person without an image gets the placeholder').toHaveAttribute('src', /person-directory-placeholder/);
    await page.screenshot({ path: 'screenshots/dir-published-page-defaults.png' });
  });

  test('Directory: the text search narrows the list to the people that match', async ({ page }) => {
    expect(assetNames.all, 'the library of the first scenario').toBeTruthy();
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishDirectoryPage(page, 'dir-search', 'all');
    await openDirectory(page, url);
    await typeSearch(page, people[2].last);
    await expect(rows(page), 'only the person that matches the search').toHaveCount(1);
    await expect(rows(page).locator('.perc-person-first-name'), 'it is Carla').toHaveText('Carla');
    await typeSearch(page, dptA);
    await expect(rows(page), 'the search also looks at the department').toHaveCount(2);
    await typeSearch(page, '');
    await expect(rows(page), 'an empty search lists everybody again').toHaveCount(4);
    await page.screenshot({ path: 'screenshots/dir-published-page-search.png' });
  });

  test('Directory: the last name letters filter the list, "Clear Filter" lists everybody, and the first letter is selected when the page opens', async ({ page }) => {
    expect(assetNames.all, 'the library of the first scenario').toBeTruthy();
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishDirectoryPage(page, 'dir-letters', 'all');
    await openDirectory(page, url, false);
    await expect(page.locator('#perc-alpha-sort-letters .perc-alpha-sort.active'), 'the first letter is selected when the page opens').toHaveCount(1);
    await expect(rows(page), 'so only the people of that letter are listed (here: Adams)').toHaveCount(1);
    await page.locator('#perc-alpha-sort-b').click();
    expect(await lastNames(page), 'letter B').toEqual([people[1].last]);
    await page.locator('#perc-alpha-sort-d').click();
    expect(await lastNames(page), 'letter D').toEqual([people[3].last]);
    await page.locator('#perc-clear-alpha-filter').click();
    await expect(rows(page), 'Clear Filter lists everybody').toHaveCount(4);
    await page.screenshot({ path: 'screenshots/dir-published-page-letters.png' });
  });

  test('Directory: for all organizations the organization filter narrows the list and offers the departments of that organization', async ({ page }) => {
    expect(assetNames.all, 'the library of the first scenario').toBeTruthy();
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishDirectoryPage(page, 'dir-org-filter', 'all');
    await openDirectory(page, url);
    await page.locator('#perc-org-filter').selectOption({ label: orgA });
    expect(await lastNames(page), 'the people of the first organization').toEqual([people[0].last, people[1].last, people[2].last].sort());
    await expect(page.locator('#perc-dpt-filter'), 'the department filter appears').toBeVisible();
    await page.locator('#perc-dpt-filter').selectOption({ label: dptB });
    expect(await lastNames(page), 'the people of the second department').toEqual([people[2].last]);
    await page.locator('#perc-org-filter').selectOption({ label: orgB });
    expect(await lastNames(page), 'the people of the second organization').toEqual([people[3].last]);
    await page.screenshot({ path: 'screenshots/dir-published-page-organization-filter.png' });
  });

  test('Directory: a directory of one organization lists its people only, without the organization filter', async ({ page }) => {
    expect(assetNames.orgA, 'the library of the first scenario').toBeTruthy();
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishDirectoryPage(page, 'dir-one-org', 'orgA');
    await openDirectory(page, url);
    expect(await lastNames(page), 'the people of the first organization').toEqual([people[0].last, people[1].last, people[2].last].sort());
    await expect(page.locator('#perc-org-filter'), 'there is no organization filter').toHaveCount(0);
    await page.screenshot({ path: 'screenshots/dir-published-page-one-organization.png' });
  });

  test('Directory: a directory of one department lists its people only', async ({ page }) => {
    expect(assetNames.dptB, 'the library of the first scenario').toBeTruthy();
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishDirectoryPage(page, 'dir-one-department', 'dptB');
    await openDirectory(page, url);
    expect(await lastNames(page), 'the person of the second department').toEqual([people[2].last]);
    await expect(page.locator('#perc-dpt-filter'), 'the department filter is not offered').toBeHidden();
    await page.screenshot({ path: 'screenshots/dir-published-page-one-department.png' });
  });

  test('Directory: the layout "Card" lists the people as cards', async ({ page }) => {
    expect(assetNames.all, 'the library of the first scenario').toBeTruthy();
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishDirectoryPage(page, 'dir-card', 'all', async f => {
      await f.locator('select[name="directoryLayoutFormat"]').selectOption('card', { timeout: 10_000 });
    });
    await openDirectory(page, url);
    await expect(list(page).locator('table'), 'no table in the card layout').toHaveCount(0);
    await expect(rows(page), 'a card for each of the four people').toHaveCount(4);
    expect(await rows(page).first().evaluate(e => e.tagName.toLowerCase()), 'cards are list items').toBe('li');
    const adams = rows(page).filter({ hasText: people[0].last });
    await expect(adams.locator('.perc-person-first-name'), 'first name').toHaveText('Alice');
    await expect(adams.locator('.perc-person-org'), 'organization').toHaveText(orgA);
    await expect(adams.locator('.perc-person-title'), 'position').toHaveText('Analyst');
    await expect(adams.locator('img'), 'the person has an image').toHaveAttribute('title', imageTitle);
    await page.screenshot({ path: 'screenshots/dir-published-page-cards.png' });
  });

  for (const value of ['p', 'div', 'h1', 'h3', 'h4', 'h5', 'h6']) {
    test(`Directory: title format "${value}" writes the directory title in a ${value} element`, async ({ page }) => {
      expect(assetNames.all, 'the library of the first scenario').toBeTruthy();
      await loginAsAdmin(page);
      await configurePublishing(page, DTS_ADMIN_URL, 'dts');
      const url = await publishDirectoryPage(page, `dir-title-${value}`, 'all', async f => {
        await f.locator('select[name="dirTitleFormat"]').selectOption(value, { timeout: 10_000 });
      });
      await page.goto(url);
      await expect(list(page).locator(`.perc-directory-name > ${value}.perc-directory-title`), `title in a ${value}`).toHaveText(assets.all);
      await page.screenshot({ path: `screenshots/dir-published-page-title-${value}.png` });
    });
  }

  test('Directory: phone, office location and the "Send Email" link are shown when "Hide" is turned off', async ({ page }) => {
    expect(assetNames.all, 'the library of the first scenario').toBeTruthy();
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishDirectoryPage(page, 'dir-contacts-shown', 'all', async f => {
      await f.locator('input[name="perc_hidefield_personPhoneField"]').uncheck();
      await f.locator('input[name="perc_hidefield_personOfficeLocation"]').uncheck();
      await f.locator('input[name="perc_hidefield_personEmailField"]').uncheck();
    });
    await openDirectory(page, url);
    const headers = (await list(page).locator('thead th').allTextContents()).map(t => t.trim()).filter(Boolean);
    expect(headers, 'the columns').toEqual(['Name', 'Organization', 'Department', 'Title', 'Phone', 'Office Location', 'Email']);
    const adams = rows(page).filter({ hasText: people[0].last });
    await expect(adams.locator('.perc-person-phone'), 'phone').toHaveText(people[0].phone);
    await expect(adams.locator('.perc-person-office-location'), 'office location').toHaveText(people[0].office);
    await expect(adams.locator('.perc-person-email a'), 'Send Email link to the form page').toHaveAttribute('href', new RegExp(`${mailForm}\\.html\\?name=Alice-${people[0].last}`));
    await page.screenshot({ path: 'screenshots/dir-published-page-contacts-shown.png' });
  });

  test('Directory: the search box, the organization filter, organization, department, position and image are left out when "Hide" is turned on', async ({ page }) => {
    expect(assetNames.all, 'the library of the first scenario').toBeTruthy();
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishDirectoryPage(page, 'dir-parts-hidden', 'all', async f => {
      for (const n of ['textSearch', 'filter_dropdown', 'personOrganization', 'personDepartment', 'personTitle', 'personImageField']) {
        await f.locator(`input[name="perc_hidefield_${n}"]`).check();
      }
    });
    await openDirectory(page, url);
    await expect(page.locator('#search-directory'), 'search box hidden').toHaveCount(0);
    await expect(page.locator('#perc-org-filter'), 'organization filter hidden').toHaveCount(0);
    const headers = (await list(page).locator('thead th').allTextContents()).map(t => t.trim()).filter(Boolean);
    expect(headers, 'only the name column is left').toEqual(['Name']);
    await expect(rows(page), 'the four people are still listed').toHaveCount(4);
    await expect(rows(page).locator('img'), 'no images').toHaveCount(0);
    await page.screenshot({ path: 'screenshots/dir-published-page-parts-hidden.png' });
  });

  test('Directory: with "Display full directory" everybody is listed when the page opens, without the first letter filter', async ({ page }) => {
    expect(assetNames.all, 'the library of the first scenario').toBeTruthy();
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishDirectoryPage(page, 'dir-full-directory', 'all', async f => {
      await f.locator('input[name="perc_display_full_dir"]').check();
    });
    await openDirectory(page, url, false);
    await expect(rows(page), 'all four people are listed on opening').toHaveCount(4);
    await page.screenshot({ path: 'screenshots/dir-published-page-full-directory.png' });
  });

  test('Directory: the custom placeholder image is shown for a person without an image, and the person with an image keeps it', async ({ page }) => {
    expect(assetNames.all, 'the library of the first scenario').toBeTruthy();
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const placeholderTitle = `QA Dir Placeholder ${run}`;
    let placeholder = '';
    for (let attempt = 1; !placeholder; attempt++) {   // the upload form sometimes refuses the file: try again
      try { placeholder = await uploadImageAsset(page, TEST_ASSET_FOLDER, IMAGE, placeholderTitle, `QA dir placeholder alt ${run}`); } catch (e) { if (attempt >= 3) throw e; }
    }
    await publishOpenItem(page);
    assetNames.placeholder = await createAssetIn(page, base, 'Directory', 'psx_cepercDirectory', { directoryTitle: `Directory Placeholder ${run}` }, async frame => {
      await chooseImageFromLibrary(page, frame.getByRole('button', { name: 'Browse' }).first(), placeholder);
    });
    const url = await publishDirectoryPage(page, 'dir-custom-placeholder', 'placeholder');
    await openDirectory(page, url);
    await expect(rows(page).filter({ hasText: people[1].last }).locator('img'), 'a person without an image shows the custom placeholder').toHaveAttribute('title', placeholderTitle);
    await expect(rows(page).filter({ hasText: people[0].last }).locator('img'), 'a person with an image keeps it').toHaveAttribute('title', imageTitle);
    await page.screenshot({ path: 'screenshots/dir-published-page-custom-placeholder.png' });
  });

  test('Directory: a directory of one organization offers a department filter for the departments of that organization', async ({ page }) => {
    expect(assetNames.orgA, 'the library of the first scenario').toBeTruthy();
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishDirectoryPage(page, 'dir-one-org-department-filter', 'orgA');
    await openDirectory(page, url);
    await expect(page.locator('#perc-dpt-filter'), 'the department filter is on the page').toBeVisible();
    await page.locator('#perc-dpt-filter').selectOption({ label: dptA });
    expect(await lastNames(page), 'the people of the first department').toEqual([people[0].last, people[1].last].sort());
    await page.screenshot({ path: 'screenshots/dir-published-page-one-organization-department-filter.png' });
  });
});
