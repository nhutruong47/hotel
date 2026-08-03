import { test } from '@playwright/test';

test('basic interactions are clickable', async ({ page }) => {
  page.on('console', (msg) => console.log(`CONSOLE_${msg.type().toUpperCase()}: ${msg.text()}`));
  page.on('pageerror', (err) => console.log(`PAGE_ERROR: ${err.message}`));

  await page.goto('http://localhost:5173/', { waitUntil: 'networkidle' });
  console.log(`URL_AFTER_LOAD=${page.url()}`);

  const availability = page.getByRole('button', { name: /check availability|kiểm tra|tìm/i }).first();
  console.log(`AVAILABILITY_VISIBLE=${await availability.isVisible().catch(() => false)}`);
  await availability.click({ timeout: 5000 });
  console.log(`MODAL_VISIBLE=${await page.getByRole('button', { name: /search|tìm|kiểm tra/i }).last().isVisible().catch(() => false)}`);

  await page.keyboard.press('Escape');
  await page.getByRole('link', { name: /login|đăng nhập/i }).first().click({ timeout: 5000 });
  await page.waitForURL(/\/login/, { timeout: 10000 });
  console.log(`URL_AFTER_LOGIN_CLICK=${page.url()}`);
});
