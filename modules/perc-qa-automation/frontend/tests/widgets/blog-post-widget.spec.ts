import { test, expect } from '@playwright/test';
import { loginAsAdmin, configurePublishing, ensureDeliveryResources } from '../helpers/cms-ui';
import { ensureBlogInfrastructure, createAndPublishPost, BLOG_FOLDER } from '../helpers/blog-ui';
import { DTS_URL, DTS_ADMIN_URL } from '../helpers/qa-env';

/**
 * Blog Post Widget: sits on the blog POST template and shows one post. The post is created the supported way (New Blog wizard and
 * "new post" button of the dashboard Blogs gadget); its title, byline and body are typed in the widget's content editor, and the
 * proof is the PUBLISHED post page served by the DTS.
 */
test.describe('Blog Post Widget', () => {
  test.setTimeout(480_000);

  test('Blog Post Widget: published post page shows the title, byline and body typed in the widget content editor', async ({ page }) => {
    const run = Date.now().toString().slice(-7);
    const title = `QA Blog Post ${run}`;
    const byline = `QA Author ${run}`;
    const bodyText = `Body text of blog post ${run} written by the QA suite.`;

    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    await ensureDeliveryResources(page);
    await ensureBlogInfrastructure(page);
    const name = await createAndPublishPost(page, title, byline, bodyText);

    await page.goto(`${DTS_URL}/${BLOG_FOLDER}/${name}.html`);
    await expect(page.getByText(title, { exact: false }).first(), 'post title on the published page').toBeVisible({ timeout: 30_000 });
    await expect(page.getByText(bodyText), 'post body on the published page').toBeVisible();
    await expect(page.getByText(byline), 'byline on the published page').toBeVisible();
    await page.screenshot({ path: 'screenshots/blog-post-widget-published-post-shows-title-byline-and-body.png' });
  });
});
