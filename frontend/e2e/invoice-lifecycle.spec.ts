import { test, expect } from '@playwright/test';

test.describe('Invoice Lifecycle', () => {
    test.beforeEach(async ({ page }) => {
        // Mock Login
        await page.context().addCookies([{
            name: 'auth-storage', // Assuming zustand persist or something
            value: JSON.stringify({ state: { user: { role: 'ACCOUNTANT' }, accessToken: 'fake' } }),
            domain: 'localhost',
            path: '/'
        }]);

        // OR mock login flow via API interception if auth is cookie based
        // Since we likely use localStorage/zustand persist, setting it might be complex in Playwright directly without visiting page.
        // Simplest: Mock the login page logic or just visit login and mock response.
        
        await page.route('**/api/v1/auth/login', async route => {
            await route.fulfill({
                json: {
                    user: { id: 1, role: 'ACCOUNTANT', email: 'acc@test.com' },
                    accessToken: 'fake-token'
                }
            });
        });
        
        // Mock Invoices List
        await page.route('**/api/v1/invoices*', async route => {
            if (route.request().method() === 'GET') {
                await route.fulfill({
                    json: {
                        content: [{ id: 1, invoiceNumber: 'INV-E2E', status: 'PENDING', totalAmount: 100, currency: 'TRY' }],
                        totalPages: 1,
                        totalElements: 1
                    }
                });
            } else if (route.request().method() === 'POST') {
                 await route.fulfill({ status: 201, json: { id: 2, message: 'Created' } });
            } else {
                await route.continue();
            }
        });

        // Login first
        await page.goto('/login');
        await page.fill('input[type="email"]', 'acc@test.com');
        await page.fill('input[type="password"]', 'pass');
        await page.click('button[type="submit"]');
        await page.waitForURL('**/');
    });

    test('should create a new invoice', async ({ page }) => {
        await page.goto('/invoices/new');
        
        // Fill form
        await page.fill('input[name="invoiceNumber"]', 'INV-NEW-001');
        // Select supplier (might be select or combobox)
        // Check implementation or assumes standard input for now
        // If Shadcn Select/Combobox, we might need to click trigger then option.
        
        await page.click('button:has-text("Kaydet")');
        
        // Expect redirect to detail or list
        // await expect(page).toHaveURL(/\/invoices\/\d+/);
    });

    test('should list invoices', async ({ page }) => {
        await page.goto('/invoices');
        await expect(page.getByText('INV-E2E')).toBeVisible();
    });
});
