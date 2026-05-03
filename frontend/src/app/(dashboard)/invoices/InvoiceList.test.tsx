import { render, screen, fireEvent, waitFor } from '@/test-utils/render'
import InvoicesPage from './page'
import { server } from '@/mocks/server'
import { http, HttpResponse } from 'msw'

// Mock the hooks if they are complex, but MSW should handle data fetching.
// However, components like 'useInvoiceFilters' might need to be in a real provider or mocked if they rely on URL state complexly.
// Since we have a real router mock and 'nuqs' or similar might be used, let's see.
// If 'useInvoiceFilters' uses 'useSearchParams', our global mock works but returns null/undefined by default.
// We might need to adjust the mock for specific tests if filters are active.

jest.mock('next/navigation', () => ({
  useRouter: () => ({
    push: jest.fn(),
    replace: jest.fn(),
    prefetch: jest.fn(),
  }),
  useSearchParams: () => ({
    get: jest.fn(),
    toString: jest.fn(() => ''),
  }),
  usePathname: () => '/invoices',
}))

describe('InvoicesPage', () => {
    
  it('renders invoice list correctly', async () => {
    render(<InvoicesPage />)
    
    // Check title
    expect(screen.getByText('invoices.title')).toBeInTheDocument()
    
    // Wait for data to load (from MSW)
    await waitFor(() => {
        expect(screen.getByText('INV-001')).toBeInTheDocument()
    })
    
    // Check table headers
    expect(screen.getByText('invoices.table.invoiceNumber')).toBeInTheDocument()
    expect(screen.getByText('invoices.table.amount')).toBeInTheDocument()
  })

  it('renders empty state when no invoices', async () => {
    // Override handler to return empty list
    server.use(
      http.get('http://localhost:3000/api/v1/invoices', () => {
        return HttpResponse.json({
          content: [],
          totalPages: 0,
          totalElements: 0,
          number: 0,
          size: 10
        })
      })
    )

    render(<InvoicesPage />)

    await waitFor(() => {
      expect(screen.getByText('invoices.empty.title')).toBeInTheDocument()
    })
  })

  it('opens export dialog on click', async () => {
      render(<InvoicesPage />)
      
      const exportBtn = screen.getByText('invoices.export')
      fireEvent.click(exportBtn)
      
      await waitFor(() => {
          expect(screen.getByRole('dialog')).toBeInTheDocument()
      })
  })
})
