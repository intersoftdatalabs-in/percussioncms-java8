import { test, expect, Page, FrameLocator } from '@playwright/test';
import * as path from 'path';
import { loginAsAdmin, createPage, publishOpenItem, configurePublishing, ensureDeliveryResources, uploadImageAsset } from '../helpers/cms-ui';
import { chooseImageFromLibrary } from '../helpers/asset-chooser';
import { placeWidget, saveLayout, openContentTab, openWidgetContentEditor, openWidgetConfigure, confirmWidgetConfigure } from '../helpers/widgets-ui';
import { DTS_URL, DTS_ADMIN_URL, TEST_FOLDER, TEST_ASSET_FOLDER } from '../helpers/qa-env';

/**
 * Image Slider: a carousel (slick) of slides, each with an image from the asset library and up to two lines of text. It is rendered by the
 * CMS when the page is published and run by script in the browser, so the proof is the PUBLISHED page served by the DTS: the slides, their
 * text, the dots and arrows, and moving from one slide to the next.
 */
const IMAGE = path.join(__dirname, '..', 'fixtures', 'qa-image.png');   // 160 x 100 PNG

test.describe.serial('Image Slider', () => {
  test.setTimeout(480_000);
  const run = Date.now().toString().slice(-7);
  let imageA = '';
  let imageB = '';

  const chooseImage = (page: Page, frame: FrameLocator, assetName: string) =>
    chooseImageFromLibrary(page, frame.locator('.perc-image-field-select-button:visible').last(), assetName);

  /** New page with an Image Slider (optionally configured) holding two slides; publishes. Returns the published URL. */
  async function publishSliderPage(page: Page, shot: string, configure?: (form: ReturnType<Page['locator']>) => Promise<void>): Promise<string> {
    const host = await createPage(page, `Slider Host ${shot.slice(-7)} ${run}`);
    await placeWidget(page, 'percImageSlider');
    if (configure) {
      const form = await openWidgetConfigure(page, 'percImageSlider');
      await configure(form);
      await page.screenshot({ path: `screenshots/${shot}-configure-dialog.png` });
      await confirmWidgetConfigure(page);
    }
    await saveLayout(page);

    const { frame, save } = await openWidgetContentEditor(page, 'percImageSlider');
    await frame.locator('#perc-content-edit-sliderName').pressSequentially(`qaslider${shot.replace(/[^a-z]/g, '').slice(-6)}${run}`, { delay: 10 });
    const slides = [
      { image: imageA, text: `Slide one ${run}`, text2: `Second line one ${run}` },
      { image: imageB, text: `Slide two ${run}`, text2: `Second line two ${run}` },
    ];
    for (const s of slides) {
      await frame.locator('.perc-table-add').click({ timeout: 15_000 });
      await chooseImage(page, frame, s.image);
      await frame.locator('input[name^="imageCaption"]:visible').nth(2 * slides.indexOf(s)).fill(s.text);
      await frame.locator('input[name^="imageCaption2"]:visible').nth(slides.indexOf(s)).fill(s.text2);
    }
    await page.screenshot({ path: `screenshots/${shot}-content-editor-two-slides.png` });
    await save.click();
    await expect(save).toBeHidden({ timeout: 60_000 });
    await openContentTab(page);
    await publishOpenItem(page);
    return `${DTS_URL}/${TEST_FOLDER}/${host}.html`;
  }

  test('Image Slider: published page runs a slider with the two slides, their text, dots and arrows, and moves to the next slide', async ({ page }) => {
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    await ensureDeliveryResources(page);
    imageA = await uploadImageAsset(page, TEST_ASSET_FOLDER, IMAGE, `QA Slide A ${run}`, `QA slide A alt ${run}`);
    await publishOpenItem(page);
    imageB = await uploadImageAsset(page, TEST_ASSET_FOLDER, IMAGE, `QA Slide B ${run}`, `QA slide B alt ${run}`);
    await publishOpenItem(page);
    expect(imageA, 'two different assets').not.toBe(imageB);

    const url = await publishSliderPage(page, 'image-slider-two-slides', async form => {
      await form.locator('input[name="perc_slide_autoplay"]').uncheck();   // the slider must not move by itself while it is checked
    });

    await page.goto(url);
    const slider = page.locator('.slick-initialized').first();
    await expect(slider, 'slider is running').toBeVisible({ timeout: 60_000 });
    const real = slider.locator('.slick-slide:not(.slick-cloned)');
    await expect(real, 'two slides').toHaveCount(2);
    await expect(slider.locator('.slick-dots li'), 'a dot per slide').toHaveCount(2);
    await expect(slider.locator('.slick-prev'), 'previous arrow').toBeVisible();
    await expect(slider.locator('.slick-next'), 'next arrow').toBeVisible();
    await expect(slider.locator('.slick-current').getByText(`Slide one ${run}`), 'first slide shown first, with its text').toBeVisible();
    await expect(slider.locator('.slick-current').getByText(`Second line one ${run}`)).toBeVisible();
    const imgContainer = slider.locator('.slick-current .perc-slide-image-container').first();
    await expect(imgContainer, 'slide image container visible').toBeVisible();
    const bgImage = await imgContainer.evaluate(e => getComputedStyle(e).backgroundImage);
    const bgUrl = bgImage.match(/url\(["']?([^"')]+)["']?\)/)?.[1];
    expect(bgUrl, `slide background-image URL (${bgImage})`).toBeTruthy();
    const imgRes = await page.request.get(new URL(bgUrl!, url).toString());
    expect(imgRes.status(), 'slide image is served by DTS').toBe(200);
    expect((await imgRes.body()).length, 'slide image has content').toBeGreaterThan(0);
    await page.screenshot({ path: 'screenshots/image-slider-published-page-first-slide.png' });

    await slider.locator('.slick-next').click();
    await expect(slider.locator('.slick-current').getByText(`Slide two ${run}`), 'next arrow moves to the second slide').toBeVisible({ timeout: 10_000 });
    // slick ignores clicks while a slide is still animating, so the click is repeated until the slide has changed
    await expect(async () => {
      await slider.locator('.slick-dots li').first().click();
      await expect(slider.locator('.slick-current').getByText(`Slide one ${run}`), 'first dot goes back to the first slide').toBeVisible({ timeout: 2_000 });
    }).toPass({ timeout: 20_000 });
    await expect(async () => {
      await slider.locator('.slick-dots li').nth(1).click();
      await expect(slider.locator('.slick-current').getByText(`Slide two ${run}`), 'second dot goes to the second slide').toBeVisible({ timeout: 2_000 });
    }).toPass({ timeout: 20_000 });
    await page.screenshot({ path: 'screenshots/image-slider-published-page-second-slide-after-next-arrow.png' });
  });

  test('Image Slider: dots and arrows turned off and the text format chosen in Configure are applied', async ({ page }) => {
    expect(imageA, 'images from the first scenario').not.toBe('');
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishSliderPage(page, 'image-slider-no-dots-arrows', async form => {
      await form.locator('input[name="perc_slide_autoplay"]').uncheck();
      await form.locator('input[name="perc_show_dots"]').uncheck();
      await form.locator('input[name="perc_show_arrows"]').uncheck();
      await form.locator('select[name="perc_slide_caption_tag_type"]').selectOption('h3', { timeout: 10_000 });
      await form.locator('select[name="perc_slide_caption_location"]').selectOption('below', { timeout: 10_000 });
    });

    await page.goto(url);
    const slider = page.locator('.slick-initialized').first();
    await expect(slider).toBeVisible({ timeout: 60_000 });
    await expect(slider.locator('.slick-slide:not(.slick-cloned)')).toHaveCount(2);
    await expect(slider.locator('.slick-dots'), 'dots are off').toHaveCount(0);
    await expect(slider.locator('.slick-prev, .slick-next'), 'arrows are off').toHaveCount(0);
    await expect(slider.locator('.slick-slide:not(.slick-cloned) h3', { hasText: `Slide one ${run}` }), 'first text line uses the chosen format (Heading 3)').toHaveCount(1);
    await page.screenshot({ path: 'screenshots/image-slider-published-page-without-dots-and-arrows.png' });
  });
});
