import { test, expect } from '@playwright/test';

test.describe('Dashboard', () => {
    test.beforeEach(async ({ page }) => {
        // Mock Login
        await page.route('**/api/v1/auth/login', async route => {
            await route.fulfill({
                json: { user: { role: 'MANAGER' }, accessToken: 'fake' }
            });
        });

        // Mock Stats
        await page.route('**/api/v1/dashboard/stats*', async route => {
             await route.fulfill({
                 json: { totalInvoices: 999, pendingCount: 5 }
             });
        });

        await page.goto('/login');
        await page.fill('input[type="email"]', 'manager@test.com');
        await page.fill('input[type="password"]', 'pass');
        await page.click('button[type="submit"]');
        await page.waitForURL('**/');
    });

    test('should display dashboard stats', async ({ page }) => {
        await expect(page.getByText('999')).toBeVisible();
        // Check for charts containers
        // e.g., "Kategori Dağılımı"
    });
});
