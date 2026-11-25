import { test, expect, Page } from '@playwright/test';
import { loginAsAdmin, createPage, publishOpenItem, configurePublishing } from '../helpers/cms-ui';
import { placeWidget, saveLayout, openContentTab, openWidgetConfigure, confirmWidgetConfigure } from '../helpers/widgets-ui';
import { createAssetIn, dragAssetOntoWidget, takeAssetToLive } from '../helpers/directory-ui';
import { DTS_URL, DTS_ADMIN_URL, TEST_FOLDER, TEST_ASSET_FOLDER } from '../helpers/qa-env';

/**
 * Department: shows a shared Department asset of the asset library on the page, with the name of its Organization (found through the folders of the library:
 * the Organization asset in the folder of the Department or the nearest one above it). Configure chooses the element of the department name and which
 * parts are hidden. The proof is the PUBLISHED page served by the DTS. The assets are created once, in a folder tree of their own:
 *   Assets / qa-assets / qa-dir-<run> / (Parent Org) / org-b / (Child Org) / dept-x / (Department)
 */
process.env.QA_DPT_RUN ??= Date.now().toString().slice(-7);   // shared with the workers Playwright restarts after a failed scenario
const run = process.env.QA_DPT_RUN;
const dir = `qa-dir-${run}`;
const folders = [TEST_ASSET_FOLDER, dir, 'org-b', 'dept-x'];
const orgName = `Child Org ${run}`;
const dpt = {
  dptName: `Dept X ${run}`, dptAddress1: '9 Side Road', dptAddress2: 'Floor 3', dptCity: 'Shelbyville', dptState: 'KY', dptPostCode: '40065', dptCountry: 'USA',
  dptPhone: '555-0200', dptFax: '555-0201', dptEmail: `dept-${run}@example.com`, dptWebsiteExt: `https://example.com/dept-${run}`,
};
let assetName = `dept-x-${run}`;   // the name the CMS gives the asset (from its title); set again by the first scenario

/** A new page with a Department widget (optionally configured), the shared Department asset dropped on it, published. Returns the published URL. */
async function publishDeptPage(page: Page, shot: string, configure?: (form: ReturnType<Page['locator']>) => Promise<void>): Promise<string> {
  const host = await createPage(page, `Dept Host ${shot.replace(/^dept-/, '')} ${run} ${Date.now().toString().slice(-4)}`);
  await placeWidget(page, 'percDepartment');
  if (configure) {
    const form = await openWidgetConfigure(page, 'percDepartment');
    await configure(form);
    await page.screenshot({ path: `screenshots/${shot}-configure-dialog.png` });
    await confirmWidgetConfigure(page);
  }
  await saveLayout(page);
  await openContentTab(page);
  await dragAssetOntoWidget(page, 'percDepartment', folders, assetName);
  await page.screenshot({ path: `screenshots/${shot}-department-on-page.png` });
  await publishOpenItem(page);
  return `${DTS_URL}/${TEST_FOLDER}/${host}.html`;
}

const box = (page: Page) => page.locator('.percDepartment');
const schemaOf = async (page: Page) => JSON.parse((await page.locator('script[type="application/ld+json"]').allTextContents()).find(t => t.includes(dpt.dptName))!);

test.describe.serial('Department', () => {
  test.setTimeout(240_000);

  test('Department: with the default options the organization, the department name in a Div linked to its page and the address are shown, phone, fax and e-mail are hidden', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    // the library: a parent organization, a child organization below it, and the department below the child
    await createAssetIn(page, [TEST_ASSET_FOLDER, dir], 'Organization', 'psx_cepercOrganization', { orgName: `Parent Org ${run}` });
    const orgAsset = await createAssetIn(page, [TEST_ASSET_FOLDER, dir, 'org-b'], 'Organization', 'psx_cepercOrganization', { orgName });
    await takeAssetToLive(page, [TEST_ASSET_FOLDER, dir, 'org-b'], orgAsset);   // the DTS pages are assembled from public assets
    assetName = await createAssetIn(page, folders, 'Department', 'psx_cepercDepartment', dpt);
    // the Organization of the Department is the one found in the folders above it
    await expect(page.frameLocator('iframe[src*="psx_cepercDepartment"]').locator('#perc-content-display-dptOrganization'), 'the Organization is found through the folders').toHaveValue(orgName, { timeout: 30_000 });
    await page.screenshot({ path: 'screenshots/dept-asset-editor-organization-found.png' });

    const url = await publishDeptPage(page, 'dept-defaults');
    await page.goto(url);
    await expect(box(page).locator('.perc-org-name'), 'organization name').toHaveText(orgName);
    await expect(box(page).locator('.perc-dpt-name a > div'), 'department name in a Div, linked').toHaveText(dpt.dptName);
    await expect(box(page).locator('.perc-dpt-name a'), 'the name links to the page of the department').toHaveAttribute('href', dpt.dptWebsiteExt);
    await expect(box(page).locator('.perc-address-street-1'), 'address line 1').toHaveText(dpt.dptAddress1);
    await expect(box(page).locator('.perc-address-street-2'), 'address line 2').toHaveText(dpt.dptAddress2);
    await expect(box(page).locator('.perc-address-city-state-post'), 'city, state and post code on one line').toHaveText('Shelbyville, KY 40065');
    await expect(box(page).locator('.perc-address-country'), 'country').toHaveText(dpt.dptCountry);
    await expect(box(page).locator('.perc-dpt-phone'), 'phone is hidden by default').toHaveCount(0);
    await expect(box(page).locator('.perc-dpt-fax'), 'fax is hidden by default').toHaveCount(0);
    await expect(box(page).locator('.perc-dpt-email'), 'e-mail is hidden by default').toHaveCount(0);
    await page.screenshot({ path: 'screenshots/dept-published-page-defaults.png' });
  });

  // every other "Format of Department Name" option
  for (const value of ['p', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6']) {
    test(`Department: name format "${value}" writes the department name in a ${value} element`, async ({ page }) => {
      expect(assetName, 'the assets of the first scenario').toBeTruthy();
      await loginAsAdmin(page);
      await configurePublishing(page, DTS_ADMIN_URL, 'dts');
      const url = await publishDeptPage(page, `dept-name-${value}`, async f => {
        await f.locator('select[name="dptNameFormat"]').selectOption(value, { timeout: 10_000 });
      });
      await page.goto(url);
      await expect(box(page).locator(`.perc-dpt-name a > ${value}`), `name in a ${value}`).toHaveText(dpt.dptName);
      await page.screenshot({ path: `screenshots/dept-published-page-name-${value}.png` });
    });
  }

  test('Department: phone, fax and e-mail are shown when "Hide" is turned off, the organization name and the address are hidden when "Hide" is turned on', async ({ page }) => {
    expect(assetName, 'the assets of the first scenario').toBeTruthy();
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishDeptPage(page, 'dept-contacts-shown-org-address-hidden', async f => {
      await f.locator('input[name="perc_hidefield_dptPhoneField"]').uncheck();
      await f.locator('input[name="perc_hidefield_dptFaxField"]').uncheck();
      await f.locator('input[name="perc_hidefield_dptEmailField"]').uncheck();
      await f.locator('input[name="perc_hidefield_orgNameField"]').check();
      await f.locator('input[name="perc_hidefield_dptAddressField"]').check();
    });
    await page.goto(url);
    await expect(box(page).locator('.perc-dpt-phone'), 'phone shown').toHaveText(dpt.dptPhone);
    await expect(box(page).locator('.perc-dpt-fax'), 'fax shown').toHaveText(dpt.dptFax);
    await expect(box(page).locator('.perc-dpt-email'), 'e-mail shown').toHaveText(dpt.dptEmail);
    await expect(box(page).locator('.perc-org-name'), 'organization name hidden').toHaveCount(0);
    await expect(box(page).locator('.perc-dpt-address'), 'address hidden').toHaveCount(0);
    await expect(box(page).locator('.perc-dpt-name'), 'the department name is still shown').toContainText(dpt.dptName);
    await page.screenshot({ path: 'screenshots/dept-published-page-contacts-shown-org-address-hidden.png' });
  });

  test('Department: "Hide Organization" removes the organization from the department', async ({ page }) => {
    expect(assetName, 'the assets of the first scenario').toBeTruthy();
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishDeptPage(page, 'dept-organization-hidden', async f => {
      await f.locator('input[name="perc_hidefield_dptOrganizationField"]').check();
    });
    await page.goto(url);
    await expect(box(page).locator('.perc-dpt-name'), 'the department name is shown').toContainText(dpt.dptName);
    await expect(box(page).locator('.perc-org-name'), 'with the organization hidden its name is not written').toHaveCount(0);
    await page.screenshot({ path: 'screenshots/dept-published-page-organization-hidden.png' });
  });

  test('Department: the schema.org description names the organization and describes the department with its address and contacts', async ({ page }) => {
    expect(assetName, 'the assets of the first scenario').toBeTruthy();
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishDeptPage(page, 'dept-schema', async f => {
      await f.locator('input[name="perc_hidefield_dptEmailField"]').uncheck();
    });
    await page.goto(url);
    const schema = await schemaOf(page);
    expect.soft(schema.department.name, 'schema.org department name').toBe(dpt.dptName);
    expect.soft(schema.department.telephone, 'schema.org department phone').toBe(dpt.dptPhone);
    expect.soft(schema.department.email, 'schema.org department e-mail').toBe(`mailto:${dpt.dptEmail}`);
    expect.soft(schema.department.address.addressLocality, 'schema.org department city').toBe(dpt.dptCity);
    expect.soft(schema.department.address.postalCode, 'schema.org department post code').toBe(dpt.dptPostCode);
    expect.soft(schema.department.address.streetAddress, 'schema.org street address has a space between the two lines').toBe('9 Side Road Floor 3');
    expect.soft(schema.name, 'schema.org name is the organization').toBe(orgName);
  });

  test('Department: "Hide Department Page" removes the link of the department name', async ({ page }) => {
    expect(assetName, 'the assets of the first scenario').toBeTruthy();
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishDeptPage(page, 'dept-page-hidden', async f => {
      await f.locator('input[name="perc_hidefield_dptPageField"]').check();
    });
    await page.goto(url);
    await expect(box(page).locator('.perc-dpt-name'), 'the department name is shown').toContainText(dpt.dptName);
    await expect(box(page).locator('.perc-dpt-name a'), 'with the page hidden there is no link').toHaveCount(0);
    await page.screenshot({ path: 'screenshots/dept-published-page-page-hidden.png' });
  });
});
