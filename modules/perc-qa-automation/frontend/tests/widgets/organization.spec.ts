import * as path from 'path';
import { test, expect, Page, FrameLocator } from '@playwright/test';
import { loginAsAdmin, createPage, publishOpenItem, configurePublishing, uploadImageAsset } from '../helpers/cms-ui';
import { chooseImageFromLibrary } from '../helpers/asset-chooser';
import { createAssetIn, dragAssetOntoWidget, takeAssetToLive } from '../helpers/directory-ui';
import { placeWidget, saveLayout, openContentTab, openWidgetContentEditor, openWidgetConfigure, confirmWidgetConfigure } from '../helpers/widgets-ui';
import { DTS_URL, DTS_ADMIN_URL, TEST_FOLDER, TEST_ASSET_FOLDER } from '../helpers/qa-env';

/**
 * Organization: shows the details of an organization (name, address, phone, fax, e-mail, logo, page link) typed in the widget's editor. Configure chooses the
 * element of the name and which parts are hidden. The proof is the PUBLISHED page served by the DTS: the elements the widget writes, and the schema.org
 * (JSON-LD) description of the organization it adds to the head of the page.
 */
process.env.QA_ORG_RUN ??= Date.now().toString().slice(-7);   // shared with the workers Playwright restarts after a failed scenario
const run = process.env.QA_ORG_RUN;

const IMAGE = path.join(__dirname, '..', 'fixtures', 'qa-image.png');   // 160 x 100 PNG
const org = {
  orgName: `QA Org ${run}`, orgAddress1: '1 Main Street', orgAddress2: 'Suite 200', orgCity: 'Springfield', orgState: 'IL', orgPostCode: '62701', orgCountry: 'USA',
  orgPhone: '555-0100', orgFax: '555-0101', orgEmail: `org-${run}@example.com`,
};

/** A new page with an Organization widget (optionally configured), its fields filled in the content editor, published. Returns the published URL. */
async function publishOrgPage(page: Page, shot: string, configure?: (form: ReturnType<Page['locator']>) => Promise<void>, extra: Record<string, string> = {}, inEditor?: (frame: FrameLocator) => Promise<void>, afterSave?: () => Promise<void>): Promise<string> {
  const host = await createPage(page, `Org Host ${shot.replace(/^org-/, '')} ${run}`);
  await placeWidget(page, 'percOrganization');
  if (configure) {
    const form = await openWidgetConfigure(page, 'percOrganization');
    await configure(form);
    await page.screenshot({ path: `screenshots/${shot}-configure-dialog.png` });
    await confirmWidgetConfigure(page);
  }
  await saveLayout(page);
  const { frame, save } = await openWidgetContentEditor(page, 'percOrganization');
  for (const [name, value] of Object.entries({ ...org, ...extra })) await frame.locator(`#perc-content-edit-${name}`).fill(value);
  if (inEditor) await inEditor(frame);
  await page.screenshot({ path: `screenshots/${shot}-content-editor.png` });
  await save.click();
  if (afterSave) await afterSave();
  await expect(save).toBeHidden({ timeout: 60_000 });
  await openContentTab(page);
  await publishOpenItem(page);
  return `${DTS_URL}/${TEST_FOLDER}/${host}.html`;
}

const box = (page: Page) => page.locator('.percOrganization');
const schemaOf = async (page: Page) => JSON.parse((await page.locator('script[type="application/ld+json"]').allTextContents()).find(t => t.includes(org.orgName))!);

test.describe('Organization', () => {
  test.setTimeout(150_000);

  test('Organization: by default the name is a Heading 2 and the address is shown, phone, fax and e-mail are hidden', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishOrgPage(page, 'org-defaults');
    await page.goto(url);
    await expect(box(page).locator('.perc-org-name h2'), 'name in a Heading 2').toHaveText(org.orgName);
    await expect(box(page).locator('.perc-org-address-1'), 'address line 1').toHaveText(org.orgAddress1);
    await expect(box(page).locator('.perc-org-address-2'), 'address line 2').toHaveText(org.orgAddress2);
    await expect(box(page).locator('.perc-org-address-city-state-post'), 'city, state and post code on one line').toHaveText('Springfield, IL 62701');
    await expect(box(page).locator('.perc-org-address-country'), 'country').toHaveText(org.orgCountry);
    await expect(box(page).locator('.perc-org-phone'), 'phone is hidden by default').toHaveCount(0);
    await expect(box(page).locator('.perc-org-fax'), 'fax is hidden by default').toHaveCount(0);
    await expect(box(page).locator('.perc-org-email'), 'e-mail is hidden by default').toHaveCount(0);
    await expect(box(page).locator('a'), 'no page is chosen, so no link').toHaveCount(0);
    const schema = await schemaOf(page);
    expect(schema['@type'], 'schema.org type').toBe('Organization');
    expect(schema.address.streetAddress, 'schema.org street address').toBe('1 Main Street Suite 200');
    expect(schema.address.addressLocality, 'schema.org city').toBe('Springfield');
    expect(schema.address.postalCode, 'schema.org post code').toBe('62701');
    await page.screenshot({ path: 'screenshots/org-published-page-defaults.png' });
  });

  // every "Format of Organization Name" option
  const formats: [string, string][] = [['h1', 'h1'], ['h3', 'h3'], ['h4', 'h4'], ['h5', 'h5'], ['h6', 'h6'], ['p', 'p'], ['div', 'div']];
  for (const [value, tag] of formats) {
    test(`Organization: name format "${value}" writes the name in a ${tag} element`, async ({ page }) => {
      await loginAsAdmin(page);
      await configurePublishing(page, DTS_ADMIN_URL, 'dts');
      const url = await publishOrgPage(page, `org-name-${value}`, async f => {
        await f.locator('select[name="orgNameFormat"]').selectOption(value, { timeout: 10_000 });
      });
      await page.goto(url);
      await expect(box(page).locator(`.perc-org-name > ${tag}`), `name in a ${tag}`).toHaveText(org.orgName);
      await page.screenshot({ path: `screenshots/org-published-page-name-${value}.png` });
    });
  }

  test('Organization: phone, fax and e-mail are shown when "Hide" is turned off, and the name and address are hidden when "Hide" is turned on', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishOrgPage(page, 'org-show-contacts-hide-name-address', async f => {
      await f.locator('input[name="perc_hidefield_organizationPhone"]').uncheck();
      await f.locator('input[name="perc_hidefield_organizationFax"]').uncheck();
      await f.locator('input[name="perc_hidefield_organizationEmail"]').uncheck();
      await f.locator('input[name="perc_hidefield_organizationName"]').check();
      await f.locator('input[name="perc_hidefield_organizationAddress"]').check();
    });
    await page.goto(url);
    await expect(box(page).locator('.perc-org-phone'), 'phone shown').toHaveText(org.orgPhone);
    await expect(box(page).locator('.perc-org-fax'), 'fax shown').toHaveText(org.orgFax);
    await expect(box(page).locator('.perc-org-email'), 'e-mail shown').toHaveText(org.orgEmail);
    await expect(box(page).locator('.perc-org-name'), 'name hidden').toHaveCount(0);
    await expect(box(page).locator('.perc-org-address'), 'address hidden').toHaveCount(0);
    await page.screenshot({ path: 'screenshots/org-published-page-contacts-shown-name-address-hidden.png' });
  });

  test('Organization: the page address typed in the editor makes the name a link, and "Hide Organization Page" removes the link', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const ext = `https://example.com/org-${run}`;
    const linked = await publishOrgPage(page, 'org-page-link', undefined, { orgPageExt: ext });
    await page.goto(linked);
    const a = box(page).locator('.perc-org-name a');
    await expect(a, 'the name links to the page').toHaveAttribute('href', ext);
    await expect(a.locator('h2'), 'the name is still in its Heading 2').toHaveText(org.orgName);
    expect((await schemaOf(page)).url, 'schema.org url of the page').toContain(ext);
    await page.screenshot({ path: 'screenshots/org-published-page-name-links-to-page.png' });
    const hidden = await publishOrgPage(page, 'org-page-link-hidden', async f => {
      await f.locator('input[name="perc_hidefield_organizationPage"]').check();
    }, { orgPageExt: ext });
    await page.goto(hidden);
    await expect(box(page).locator('.perc-org-name'), 'the name is shown').toHaveText(org.orgName);
    await expect(box(page).locator('a'), 'with the page hidden there is no link').toHaveCount(0);
  });

  test('Organization: the logo chosen in the editor is shown, linked to the page of the organization, and named in the schema', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const logoName = await uploadImageAsset(page, TEST_ASSET_FOLDER, IMAGE, `QA Org Logo ${run}`, `QA org logo alt ${run}`);
    await publishOpenItem(page);
    const ext = `https://example.com/org-logo-${run}`;
    const url = await publishOrgPage(page, 'org-logo', undefined, { orgPageExt: ext }, async frame => {
      await chooseImageFromLibrary(page, frame.getByRole('button', { name: 'Browse' }).first(), logoName);
    });
    await page.goto(url);
    const img = box(page).locator('.perc-org-logo img');
    await expect(img, 'the logo is shown').toHaveAttribute('alt', `QA org logo alt ${run}`);
    await expect(img, 'title of the logo image').toHaveAttribute('title', `QA Org Logo ${run}`);
    expect(await img.evaluate((e: HTMLImageElement) => e.naturalWidth), 'the logo is a real image').toBeGreaterThan(0);
    await expect(box(page).locator('.perc-org-logo a'), 'the logo links to the page of the organization').toHaveAttribute('href', ext);
    expect(String((await schemaOf(page)).image), 'schema.org logo url').toContain(logoName);
    await page.screenshot({ path: 'screenshots/org-published-page-logo.png' });
    const hidden = await publishOrgPage(page, 'org-logo-hidden', async f => {
      await f.locator('input[name="perc_hidefield_organizationLogo"]').check();
    }, undefined, async frame => {
      await chooseImageFromLibrary(page, frame.getByRole('button', { name: 'Browse' }).first(), logoName);
    });
    await page.goto(hidden);
    await expect(box(page).locator('.perc-org-logo'), 'with Hide Organization Logo on, no logo is written').toHaveCount(0);
  });

  test('Organization: the parent organization, found through the folders of the library, is named in the schema', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    // the parent of an Organization is the Organization in the folder above its own folder
    const dir = `qa-dir-${run}`;
    const parentName = `Parent Org ${run}`;
    const parentAsset = await createAssetIn(page, [TEST_ASSET_FOLDER, dir], 'Organization', 'psx_cepercOrganization', { orgName: parentName });
    await takeAssetToLive(page, [TEST_ASSET_FOLDER, dir], parentAsset);
    const childFolders = [TEST_ASSET_FOLDER, dir, 'org-b'];
    const childAsset = await createAssetIn(page, childFolders, 'Organization', 'psx_cepercOrganization', { orgName: org.orgName, orgAddress1: org.orgAddress1 });
    const editor = page.frameLocator('iframe[src*="psx_cepercOrganization"]');
    await expect(editor.locator('#perc-content-display-parentOrganization'), 'the parent is found in the folder above').toHaveValue(parentName, { timeout: 30_000 });
    await page.screenshot({ path: 'screenshots/org-asset-editor-parent-organization-found.png' });
    // the link to the parent is filled in when the editor opens: save once more to store it
    await page.getByRole('button', { name: 'Save' }).click();
    await page.waitForTimeout(4_000);

    const host = await createPage(page, `Org Host parent ${run} ${Date.now().toString().slice(-4)}`);
    await placeWidget(page, 'percOrganization');
    await saveLayout(page);
    await openContentTab(page);
    await dragAssetOntoWidget(page, 'percOrganization', childFolders, childAsset);
    await publishOpenItem(page);
    await page.goto(`${DTS_URL}/${TEST_FOLDER}/${host}.html`);
    await expect(box(page).locator('.perc-org-name'), 'the organization of the dropped asset is shown').toHaveText(org.orgName);
    expect((await schemaOf(page)).parentOrganization, 'schema.org parent organization').toBe(parentName);
    await page.screenshot({ path: 'screenshots/org-published-page-parent-organization.png' });
  });
});
