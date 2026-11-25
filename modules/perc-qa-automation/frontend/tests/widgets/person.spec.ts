import * as path from 'path';
import { test, expect, Page } from '@playwright/test';
import { loginAsAdmin, createPage, publishOpenItem, configurePublishing, uploadImageAsset } from '../helpers/cms-ui';
import { chooseImageFromLibrary } from '../helpers/asset-chooser';
import { placeWidget, saveLayout, openContentTab, openWidgetConfigure, confirmWidgetConfigure } from '../helpers/widgets-ui';
import { createAssetIn, dragAssetOntoWidget, takeAssetToLive, choosePageFromTree } from '../helpers/directory-ui';
import { DTS_URL, DTS_ADMIN_URL, TEST_FOLDER, TEST_ASSET_FOLDER } from '../helpers/qa-env';

/**
 * Person: shows a shared Person asset of the asset library on the page: name, the Organization and Department found through the folders of the library,
 * position, phone, office location, a "Send Email" link to a form page, the profile image and a caption. Configure chooses which parts are hidden.
 * The proof is the PUBLISHED page served by the DTS. The assets are created once, in a folder tree of their own:
 *   Assets / qa-assets / qa-dir-<run> / org-b / (Child Org) / dept-x / (Department, Person)
 */
process.env.QA_PERSON_RUN ??= Date.now().toString().slice(-7);   // shared with the workers Playwright restarts after a failed scenario
const run = process.env.QA_PERSON_RUN;
const IMAGE = path.join(__dirname, '..', 'fixtures', 'qa-image.png');   // 160 x 100 PNG
const dir = `qa-dir-${run}`;
const folders = [TEST_ASSET_FOLDER, dir, 'org-b', 'dept-x'];
const orgName = `Child Org ${run}`;
const dptName = `Dept X ${run}`;
const person = {
  honorificPrefix: 'Dr.', personFirstName: 'Quinn', personLastName: `Person${run}`, personTitle: 'Lead Tester', personPhone: '555-0300',
  personEmail: `quinn-${run}@example.com`, personOfficeLocation: 'Room 12',
};
const fullName = `${person.honorificPrefix} ${person.personFirstName} ${person.personLastName}`;
const caption = `QA person caption ${run}`;
const logoTitle = `QA Person Photo ${run}`;
const logoAlt = `QA person photo alt ${run}`;
let assetName = `quinn-person${run}`;   // the name the CMS gives the asset (from its title); set again by the first scenario
let formPage = '';
let personalPage = '';

/** A plain published page (the target of the personal page link and of the e-mail form link). Returns its file name without extension. */
async function publishPlainPage(page: Page, title: string): Promise<string> {
  const name = await createPage(page, title);
  await openContentTab(page);
  await publishOpenItem(page);
  return name;
}

/** A new page with a Person widget (optionally configured), the shared Person asset dropped on it, published. Returns the published URL. */
async function publishPersonPage(page: Page, shot: string, configure?: (form: ReturnType<Page['locator']>) => Promise<void>): Promise<string> {
  const host = await createPage(page, `Person Host ${shot.replace(/^person-/, '')} ${run} ${Date.now().toString().slice(-4)}`);
  await placeWidget(page, 'percPerson');
  if (configure) {
    const form = await openWidgetConfigure(page, 'percPerson');
    await configure(form);
    await page.screenshot({ path: `screenshots/${shot}-configure-dialog.png` });
    await confirmWidgetConfigure(page);
  }
  await saveLayout(page);
  await openContentTab(page);
  await dragAssetOntoWidget(page, 'percPerson', folders, assetName);
  await page.screenshot({ path: `screenshots/${shot}-person-on-page.png` });
  await publishOpenItem(page);
  return `${DTS_URL}/${TEST_FOLDER}/${host}.html`;
}

const box = (page: Page) => page.locator('.percPerson');
const schemaOf = async (page: Page) => JSON.parse((await page.locator('script[type="application/ld+json"]').allTextContents()).find(t => t.includes(person.personLastName))!);

test.describe.serial('Person', () => {
  test.setTimeout(300_000);

  test('Person: with the default options the name, organization, department, position, image and caption are shown, phone, office location and e-mail are hidden', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    // the library: a child organization and a department (both public: the DTS pages are assembled from public assets) and the person in the department folder
    await createAssetIn(page, [TEST_ASSET_FOLDER, dir], 'Organization', 'psx_cepercOrganization', { orgName: `Parent Org ${run}` });
    const orgAsset = await createAssetIn(page, [TEST_ASSET_FOLDER, dir, 'org-b'], 'Organization', 'psx_cepercOrganization', { orgName });
    await takeAssetToLive(page, [TEST_ASSET_FOLDER, dir, 'org-b'], orgAsset);
    const dptAsset = await createAssetIn(page, folders, 'Department', 'psx_cepercDepartment', { dptName });
    await takeAssetToLive(page, folders, dptAsset);
    // the pages and the image the person points to
    formPage = await publishPlainPage(page, `AA Person Mail Form ${run}`);
    personalPage = await publishPlainPage(page, `AA Person Own Page ${run}`);
    const image = await uploadImageAsset(page, TEST_ASSET_FOLDER, IMAGE, logoTitle, logoAlt);
    await publishOpenItem(page);

    assetName = await createAssetIn(page, folders, 'Person', 'psx_cepercPerson', person, async frame => {
      await choosePageFromTree(page, frame.getByRole('button', { name: 'Browse' }).nth(0), formPage);
      await chooseImageFromLibrary(page, frame.getByRole('button', { name: 'Browse' }).nth(1), image);
      await choosePageFromTree(page, frame.getByRole('button', { name: 'Browse' }).nth(2), personalPage);
      await frame.frameLocator('.tox-edit-area__iframe').first().locator('body').click({ timeout: 15_000 });
      await page.keyboard.type(caption);
    });
    const f = page.frameLocator('iframe[src*="psx_cepercPerson"]');
    await expect(f.locator('#perc-content-display-personOrganization'), 'the Organization is found through the folders').toHaveValue(orgName, { timeout: 30_000 });
    await expect(f.locator('#perc-content-display-personDepartment'), 'the Department is found in the folder of the person').toHaveValue(dptName, { timeout: 30_000 });
    await page.screenshot({ path: 'screenshots/person-asset-editor-organization-department-found.png' });
    // the links to the organization and the department are filled in when the editor opens: save once more to store them
    await page.getByRole('button', { name: 'Save' }).click();
    await page.waitForTimeout(4_000);

    const url = await publishPersonPage(page, 'person-defaults');
    await page.goto(url);
    await expect(box(page).locator('.perc-person-name'), 'prefix, first and last name').toHaveText(fullName);
    await expect(box(page).locator('a:has(> .perc-person-name)'), 'the name links to the personal page').toHaveAttribute('href', new RegExp(`${personalPage}\\.html$`));
    await expect(box(page).locator('.perc-person-org p'), 'organization').toHaveText(orgName);
    await expect(box(page).locator('.perc-person-dpt p'), 'department').toHaveText(dptName);
    await expect(box(page).locator('.perc-person-title p'), 'position').toHaveText(person.personTitle);
    await expect(box(page).locator('.perc-person-image img'), 'profile image alternative text').toHaveAttribute('alt', logoAlt);
    await expect(box(page).locator('.perc-person-image img'), 'profile image title').toHaveAttribute('title', logoTitle);
    expect(await box(page).locator('.perc-person-image img').evaluate((e: HTMLImageElement) => e.naturalWidth), 'the image is a real image').toBeGreaterThan(0);
    await expect(box(page).locator('.perc-person-image a'), 'the image links to the personal page').toHaveAttribute('href', new RegExp(`${personalPage}\\.html$`));
    await expect(box(page).locator('.perc-person-caption'), 'caption').toContainText(caption);
    await expect(box(page).locator('.perc-person-phone'), 'phone is hidden by default').toHaveCount(0);
    await expect(box(page).locator('.perc-person-office-location'), 'office location is hidden by default').toHaveCount(0);
    await expect(box(page).locator('.perc-person-email'), 'the Send Email link is hidden by default').toHaveCount(0);
    await page.screenshot({ path: 'screenshots/person-published-page-defaults.png' });
  });

  test('Person: phone, office location and the "Send Email" link are shown when "Hide" is turned off', async ({ page }) => {
    expect(formPage, 'the pages of the first scenario').toBeTruthy();
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishPersonPage(page, 'person-contacts-shown', async f => {
      await f.locator('input[name="perc_hidefield_personPhoneField"]').uncheck();
      await f.locator('input[name="perc_hidefield_personOfficeLocation"]').uncheck();
      await f.locator('input[name="perc_hidefield_personEmailField"]').uncheck();
    });
    await page.goto(url);
    await expect(box(page).locator('.perc-person-phone p'), 'phone shown').toHaveText(person.personPhone);
    await expect(box(page).locator('.perc-person-office-location p'), 'office location shown').toHaveText(person.personOfficeLocation);
    const mail = box(page).locator('.perc-person-email a');
    await expect(mail, 'Send Email link').toHaveText('Send Email');
    await expect(mail, 'the link goes to the e-mail form page with the name of the person').toHaveAttribute('href', new RegExp(`${formPage}\\.html\\?name=Quinn-Person${run}$`));
    await page.screenshot({ path: 'screenshots/person-published-page-contacts-shown.png' });
  });

  test('Person: organization, department, position, image, personal page and caption are left out when "Hide" is turned on', async ({ page }) => {
    expect(formPage, 'the pages of the first scenario').toBeTruthy();
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishPersonPage(page, 'person-parts-hidden', async f => {
      for (const n of ['personOrgField', 'personDptField', 'personTitleField', 'personImageField', 'personPageField', 'personCaptionField']) {
        await f.locator(`input[name="perc_hidefield_${n}"]`).check();
      }
    });
    await page.goto(url);
    await expect(box(page).locator('.perc-person-name'), 'the name is still shown').toHaveText(fullName);
    await expect(box(page).locator('a:has(> .perc-person-name)'), 'with the personal page hidden the name is not a link').toHaveCount(0);
    await expect(box(page).locator('.perc-person-org'), 'organization hidden').toHaveCount(0);
    await expect(box(page).locator('.perc-person-dpt'), 'department hidden').toHaveCount(0);
    await expect(box(page).locator('.perc-person-title'), 'position hidden').toHaveCount(0);
    await expect(box(page).locator('.perc-person-image'), 'image hidden').toHaveCount(0);
    await expect(box(page).locator('.perc-person-caption'), 'caption hidden').toHaveCount(0);
    await page.screenshot({ path: 'screenshots/person-published-page-parts-hidden.png' });
  });

  test('Person: the schema.org description names the person, the organization, the position, the image and the contacts', async ({ page }) => {
    expect(formPage, 'the pages of the first scenario').toBeTruthy();
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishPersonPage(page, 'person-schema');
    await page.goto(url);
    const schema = await schemaOf(page);
    expect.soft(schema['@type'], 'schema.org type').toBe('Person');
    expect.soft(schema.givenName, 'given name').toBe(person.personFirstName);
    expect.soft(schema.familyName, 'family name').toBe(person.personLastName);
    expect.soft(schema.honorificPrefix, 'honorific prefix').toBe(person.honorificPrefix);
    expect.soft(schema.worksFor, 'works for the organization').toBe(orgName);
    expect.soft(schema.jobTitle, 'job title').toBe(person.personTitle);
    expect.soft(schema.email, 'e-mail').toBe(`mailto:${person.personEmail}`);
    expect.soft(schema.telephone, 'phone').toBe(person.personPhone);
    expect.soft(schema.image?.alternateName, 'image alternative text').toBe(logoAlt);
    expect.soft(String(schema.url), 'url of the personal page').toContain(`${personalPage}.html`);
  });
});
