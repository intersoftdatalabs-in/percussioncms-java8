import { test, expect } from '@playwright/test';
import { loginAsAdmin, configurePublishing, ensureDeliveryResources } from '../helpers/cms-ui';
import { ensureBlogInfrastructure, createAndPublishPost, publishBlogIndex, BLOG_FOLDER } from '../helpers/blog-ui';
import { DTS_URL, DTS_ADMIN_URL } from '../helpers/qa-env';

/**
 * Blog List Widget: sits on the blog INDEX page and lists the published posts of the blog. The list is built in the browser from the
 * DTS blog service, so the proof is the PUBLISHED index page served by the DTS: the three posts created here are listed, newest first,
 * each with its byline and a link that opens that post.
 */
test.describe('Blog List Widget', () => {
  test.setTimeout(600_000);

  test('Blog List Widget: published blog index lists the posts with their bylines, newest first, and each links to its post', async ({ page }) => {
    const run = Date.now().toString().slice(-7);
    const posts = ['First', 'Second', 'Third'].map(n => ({ title: `QA List Post ${n} ${run}`, byline: `QA Author ${n} ${run}`, body: `Body of the ${n} list post ${run}.`, name: '' }));

    await loginAsAdmin(page);
    await configurePublishing(page, DTS_ADMIN_URL, 'dts');
    await ensureDeliveryResources(page);
    await ensureBlogInfrastructure(page);
    for (const p of posts) p.name = await createAndPublishPost(page, p.title, p.byline, p.body);
    await publishBlogIndex(page);

    await page.goto(`${DTS_URL}/${BLOG_FOLDER}/index.html`);
    for (const p of posts) await expect(page.getByText(p.title, { exact: false }).first(), `"${p.title}" in the published list`).toBeVisible({ timeout: 60_000 });
    for (const p of posts) await expect(page.getByText(p.byline).first(), `byline of "${p.title}"`).toBeVisible();

    // newest first: the post created last is listed above the one created before it
    const y = async (t: string) => (await page.getByText(t, { exact: false }).first().boundingBox())!.y;
    expect(await y(posts[2].title), 'third post above second').toBeLessThan(await y(posts[1].title));
    expect(await y(posts[1].title), 'second post above first').toBeLessThan(await y(posts[0].title));
    await page.screenshot({ path: 'screenshots/blog-list-widget-published-index-lists-three-posts-newest-first.png', fullPage: true });

    // the entry links to its post
    await page.locator(`a[href*="${posts[0].name}"]`).first().click();
    await expect(page.getByText(posts[0].body)).toBeVisible({ timeout: 30_000 });
    await page.screenshot({ path: 'screenshots/blog-list-widget-link-opens-the-post-page.png' });
  });
});
