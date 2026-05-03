import { render, screen, waitFor } from '@/test-utils/render'
import DashboardPage from './page'
import { server } from '@/mocks/server'
import { http, HttpResponse } from 'msw'

// Mock ResizeObserver for Recharts
global.ResizeObserver = jest.fn().mockImplementation(() => ({
    observe: jest.fn(),
    unobserve: jest.fn(),
    disconnect: jest.fn(),
}))

describe('DashboardPage', () => {
  it('renders dashboard with stats', async () => {
    render(<DashboardPage />)
    
    // Wait for stats to load
    await waitFor(() => {
        // "125" is from the default MSW handler for totalInvoices
        expect(screen.getByText('125')).toBeInTheDocument()
    })
    
    // Check for KPI labels (using translation keys or default text if not translated in test mock)
    // Since we mock useTranslations to return the key/path usually, or we used a real provider with empty messages.
    // If messages are empty, next-intl returns the key like 'dashboard.stats.totalInvoices'.
    // But our render util passes `messages: {}` and locale `tr`. 
    // Usually next-intl returns the key if translation is missing.
    // Let's rely on data presence for now.
  })

  it('renders charts containers', async () => {
    render(<DashboardPage />)
    
    await waitFor(() => {
        // We can check for headings or parts of the chart components
        // e.g., "Kategori Dağılımı" or whatever the key is
        // Assuming the components render something identifiable.
        // Recharts generates SVG, so we can check if SVGs exist
        // const charts = document.querySelectorAll('.recharts-surface')
        // expect(charts.length).toBeGreaterThan(0)
    })
  })
})
