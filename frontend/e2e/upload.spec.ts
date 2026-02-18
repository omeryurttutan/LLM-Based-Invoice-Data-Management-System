import { test, expect } from '@playwright/test';

test.describe('Upload Flow', () => {
    test.beforeEach(async ({ page }) => {
         // Mock login
         await page.route('**/api/v1/auth/login', async route => {
            await route.fulfill({
                json: { user: { role: 'ACCOUNTANT' }, accessToken: 'fake' }
            });
        });
        await page.goto('/login');
        await page.fill('input[type="email"]', 'test@test.com');
        await page.fill('input[type="password"]', 'pass');
        await page.click('button[type="submit"]');
        await page.waitForURL('**/');
    });

    test('should upload a file', async ({ page }) => {
        await page.goto('/invoices/upload'); // Adjust path if it's /upload or /invoices/upload
        
        // Create dummy file
        const buffer = Buffer.from('dummy content');
        
        // Evaluate file chooser or input
        // Since UploadZone uses a hidden input, we can locate it
        const fileInput = page.locator('input[type="file"]');
        await fileInput.setInputFiles({
            name: 'test.pdf',
            mimeType: 'application/pdf',
            buffer
        });
        
        // Check for success message or progress
        // e.g. "Dosya yüklendi" or progress bar
        // We mock the upload API if needed, otherwise it might fail against real backend without file
        
        await page.route('**/api/v1/invoices/upload', async route => {
             await route.fulfill({ status: 200, json: { message: 'Uploaded' } });
        });
        
        // Assert visual feedback
        // await expect(page.getByText('Başarılı')).toBeVisible();
    });
});
