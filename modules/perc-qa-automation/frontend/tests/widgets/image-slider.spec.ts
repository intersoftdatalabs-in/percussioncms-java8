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
  async function publishSliderPage(page: Page, shot: string, configure?: (form: ReturnType<Page['locator']>) => Promise<void>, firstSlideLink?: string): Promise<string> {
    const host = await createPage(page, `Slider Host ${shot.replace(/^image-slider-/, "")} ${run}`);
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
      if (firstSlideLink && slides.indexOf(s) === 0) {
        await frame.locator('select.perc-slider-image-link-setting-select:visible').first().selectOption('external');
        await frame.locator('input.perc-slider-image-external-link:visible').first().fill(firstSlideLink);
      }
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

  /** The options slick was started with on the published page. */
  const slickOptions = (page: Page) => page.evaluate(() => (window as any).jQuery('.perc-image-slider').first().slick('getSlick').options);

  test('Image Slider: the text over the slide with its second line format, alignment, slide height and light theme are applied', async ({ page }) => {
    expect(imageA, 'images from the first scenario').not.toBe('');
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishSliderPage(page, 'image-slider-text-over-slide', async form => {
      await form.locator('input[name="perc_slide_autoplay"]').uncheck();
      await form.locator('select[name="perc_slide_caption_location"]').selectOption('over', { timeout: 10_000 });
      await form.locator('select[name="perc_slide_caption_2_tag_type"]').selectOption('h4', { timeout: 10_000 });
      await form.locator('select[name="perc_slide_caption_alignment"]').selectOption('center', { timeout: 10_000 });
      await form.locator('select[name="perc_slide_caption_vertical_alignment"]').selectOption('bottom', { timeout: 10_000 });
      await form.locator('input[name="perc_slide_image_height"]').fill('300px');
      await form.locator('select[name="perc_slide_theme"]').selectOption('light', { timeout: 10_000 });
    });
    await page.goto(url);
    const slider = page.locator('.slick-initialized').first();
    await expect(slider).toBeVisible({ timeout: 60_000 });
    const current = slider.locator('.slick-current');
    await expect(current.locator('.perc-slide-image-container .perc-slide-caption-overlay-container-vertical-bottom'), 'text is inside the image, at the bottom').toHaveCount(1);
    await expect(current.locator('.perc-slide-image-caption.perc-slide-caption-horizontal-center', { hasText: `Slide one ${run}` }), 'first line centred').toHaveCount(1);
    await expect(current.locator('h4.perc-slide-image-caption-2', { hasText: `Second line one ${run}` }), 'second line is a Heading 4').toHaveCount(1);
    await expect(current.locator('.perc-slide-caption-container'), 'no text above or below the image').toHaveCount(0);
    expect(await current.locator('.perc-slide-image-container').evaluate(e => getComputedStyle(e).height), 'slide height').toBe('300px');
    expect(await slider.locator('.slick-next').evaluate(e => getComputedStyle(e, '::before').color), 'light theme: white arrows').toBe('rgb(255, 255, 255)');
    await page.screenshot({ path: 'screenshots/image-slider-published-page-text-over-slide-light-theme.png' });
  });

  test('Image Slider: the slide link opens in the chosen target, and with "Images are links" off only the text is linked', async ({ page }) => {
    expect(imageA, 'images from the first scenario').not.toBe('');
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const link = `https://example.com/qa-slide-${run}`;
    const linked = await publishSliderPage(page, 'image-slider-link-target-self', async form => {
      await form.locator('input[name="perc_slide_autoplay"]').uncheck();
      await form.locator('select[name="perc_slide_targetblank"]').selectOption('_self', { timeout: 10_000 });
    }, link);
    await page.goto(linked);
    const slider = page.locator('.slick-initialized').first();
    await expect(slider).toBeVisible({ timeout: 60_000 });
    const first = slider.locator('.slick-slide:not(.slick-cloned)').first();
    await expect(first.locator(`a[href="${link}"] .perc-slide-image-container`), 'with "Images are links" on, the link wraps the image').toHaveCount(1);
    await expect(first.locator(`a[href="${link}"]`).first(), 'link target from Configure').toHaveAttribute('target', '_self');
    await expect(slider.locator('.slick-slide:not(.slick-cloned)').nth(1).locator('a'), 'the slide without a link has none').toHaveCount(0);
    await page.screenshot({ path: 'screenshots/image-slider-published-page-slide-link-target-self.png' });

    const textOnly = await publishSliderPage(page, 'image-slider-images-not-links', async form => {
      await form.locator('input[name="perc_slide_autoplay"]').uncheck();
      await form.locator('input[name="perc_slide_image_link"]').uncheck();
      await form.locator('select[name="perc_slide_targetblank"]').selectOption('_blank', { timeout: 10_000 });
    }, link);
    await page.goto(textOnly);
    const slider2 = page.locator('.slick-initialized').first();
    await expect(slider2).toBeVisible({ timeout: 60_000 });
    const first2 = slider2.locator('.slick-slide:not(.slick-cloned)').first();
    await expect(first2.locator(`a[href="${link}"] .perc-slide-image-container`), 'the image is not inside a link').toHaveCount(0);
    await expect(first2.locator(`.perc-slide-caption-container a[href="${link}"]`).first(), 'the text is the link').toHaveAttribute('target', '_blank');
    await page.screenshot({ path: 'screenshots/image-slider-published-page-only-text-linked.png' });
  });

  test('Image Slider: slides to show per screen size and the breakpoints from Configure decide how many slides are shown', async ({ page }) => {
    expect(imageA, 'images from the first scenario').not.toBe('');
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishSliderPage(page, 'image-slider-slides-per-screen', async form => {
      await form.locator('input[name="perc_slide_autoplay"]').uncheck();
      await form.locator('input[name="perc_slide_show_number_desktop"]').fill('2');
      await form.locator('input[name="perc_slide_show_number_tablet"]').fill('1');
      await form.locator('input[name="perc_slide_show_number_mobile"]').fill('2');
      await form.locator('input[name="perc_slider_tablet_breakpoint"]').fill('1000');
      await form.locator('input[name="perc_slider_mobile_breakpoint"]').fill('500');
    });
    for (const [width, shown, what] of [[1280, 2, 'desktop'], [800, 1, 'tablet (below the 1000 breakpoint)'], [400, 2, 'mobile (below the 500 breakpoint)']] as const) {
      await page.setViewportSize({ width, height: 900 });
      await page.goto(url);
      await expect(page.locator('.slick-initialized').first()).toBeVisible({ timeout: 60_000 });
      await expect(page.locator('.slick-initialized .slick-slide.slick-active'), `slides shown on ${what}`).toHaveCount(shown, { timeout: 10_000 });
      await page.screenshot({ path: `screenshots/image-slider-published-page-${width}px-${shown}-slides.png` });
    }
  });

  test('Image Slider: fade, autoplay with its speed, scroll speed and slides to scroll are applied and the slider moves by itself', async ({ page }) => {
    expect(imageA, 'images from the first scenario').not.toBe('');
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishSliderPage(page, 'image-slider-fade-autoplay-speeds', async form => {
      await form.locator('input[name="perc_slide_autoplay"]').check();
      await form.locator('input[name="perc_slide_fade"]').check();
      await form.locator('input[name="perc_autoplay_speed"]').fill('1500');
      await form.locator('input[name="perc_scroll_speed"]').fill('300');
      await form.locator('input[name="perc_slide_scroll_number"]').fill('1');
    });
    await page.goto(url);
    const slider = page.locator('.slick-initialized').first();
    await expect(slider).toBeVisible({ timeout: 60_000 });
    const o = await slickOptions(page);
    expect(o.fade, 'fade effect').toBe(true);
    expect(o.autoplay, 'autoplay').toBe(true);
    expect(o.autoplaySpeed, 'autoplay speed').toBe(1500);
    expect(o.speed, 'scroll speed').toBe(300);
    expect(o.slidesToScroll, 'slides to scroll').toBe(1);
    await expect(slider.locator('.slick-current').getByText(`Slide two ${run}`), 'the slider moves to the next slide on its own').toBeVisible({ timeout: 15_000 });
    await page.screenshot({ path: 'screenshots/image-slider-published-page-autoplay-moved-to-second-slide.png' });
  });

  test('Image Slider: the navigation layout shows thumbnails of the chosen height next to the main slider', async ({ page }) => {
    expect(imageA, 'images from the first scenario').not.toBe('');
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const url = await publishSliderPage(page, 'image-slider-navigation-layout', async form => {
      await form.locator('input[name="perc_slide_autoplay"]').uncheck();
      await form.locator('select[name="perc_slide_layout"]').selectOption('navigation', { timeout: 10_000 });
      await form.locator('input[name="perc_thumbnail_height"]').fill('60px');
    });
    await page.goto(url);
    await expect(page.locator('.slick-initialized').first()).toBeVisible({ timeout: 60_000 });
    const nav = page.locator('.perc-image-slider[class*="-navigation"]');
    await expect(nav, 'the thumbnail strip').toBeVisible();
    await expect(nav.locator('.perc-slide-thumbnail-container').first(), 'thumbnail height from Configure').toBeVisible();
    expect(await nav.locator('.perc-slide-thumbnail-container').first().evaluate(e => getComputedStyle(e).height), 'thumbnail height').toBe('60px');
    const main = page.locator('.perc-image-slider:not([class*="-navigation"])').first();
    expect((await slickOptions(page)).fade, 'the main slider fades in the navigation layout').toBe(true);
    await expect(main.locator('.slick-dots, .slick-prev, .slick-next'), 'the main slider has no dots or arrows').toHaveCount(0);
    await page.screenshot({ path: 'screenshots/image-slider-published-page-navigation-layout.png' });
  });

  test('Image Slider: custom arrow classes from Configure are written on the arrows', async ({ page }) => {
    expect(imageA, 'images from the first scenario').not.toBe('');
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const arrows = await publishSliderPage(page, 'image-slider-custom-arrows', async form => {
      await form.locator('input[name="perc_slide_autoplay"]').uncheck();
      await form.locator('input[name="perc_enable_custom_arrows"]').check();
      await form.locator('input[name="perc_prev_arrow_class"]').fill('fa fa-chevron-left');
      await form.locator('input[name="perc_next_arrow_class"]').fill('fa fa-chevron-right');
    });
    await page.goto(arrows);
    const slider = page.locator('.slick-initialized').first();
    await expect(slider).toBeVisible({ timeout: 60_000 });
    await expect(slider.locator('.slick-prev.fa.fa-chevron-left'), 'previous arrow class').toHaveCount(1);
    await expect(slider.locator('.slick-next.fa.fa-chevron-right'), 'next arrow class').toHaveCount(1);
    await page.screenshot({ path: 'screenshots/image-slider-published-page-custom-arrows.png' });
  });

  test('Image Slider: a zero for slides to show writes a warning instead of the slider', async ({ page }) => {
    expect(imageA, 'images from the first scenario').not.toBe('');
    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    const zero = await publishSliderPage(page, 'image-slider-zero-slides-to-show', async form => {
      await form.locator('input[name="perc_slide_show_number_desktop"]').fill('0');
    });
    await page.goto(zero);
    await expect(page.getByText('Slider warning', { exact: false }), 'warning instead of the slider').toBeVisible({ timeout: 30_000 });
    await expect(page.locator('.slick-initialized'), 'no slider is started').toHaveCount(0);
    await page.screenshot({ path: 'screenshots/image-slider-published-page-zero-slides-warning.png' });
  });
});
