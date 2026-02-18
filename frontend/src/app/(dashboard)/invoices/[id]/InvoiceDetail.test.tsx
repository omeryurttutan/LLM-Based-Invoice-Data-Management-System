import { render, screen, waitFor, fireEvent } from '@/test-utils/render'
import InvoiceDetailPage from './page'
import { server } from '@/mocks/server'
import { http, HttpResponse } from 'msw'

// Mock ResizeObserver
global.ResizeObserver = jest.fn().mockImplementation(() => ({
    observe: jest.fn(),
    unobserve: jest.fn(),
    disconnect: jest.fn(),
}))

const mockPush = jest.fn()
jest.mock('next/navigation', () => ({
  useRouter: () => ({
    push: mockPush,
    refresh: jest.fn(),
  }),
  useSearchParams: () => ({
    get: jest.fn(),
  }),
}))

describe('InvoiceDetailPage', () => {
  const params = { id: '1' }

  it('renders invoice details correctly', async () => {
    render(<InvoiceDetailPage params={params} />)
    
    await waitFor(() => {
        expect(screen.getByText('INV-001')).toBeInTheDocument()
        expect(screen.getByText('Tech Corp')).toBeInTheDocument()
    })
  })

  it('renders 404 state when invoice not found', async () => {
    // Override handler for 404
      server.use(
        http.get('http://localhost:3000/api/v1/invoices/999', () => {
          return new HttpResponse(null, { status: 404 })
        })
      )
      
      render(<InvoiceDetailPage params={{ id: '999' }} />)
      
      await waitFor(() => {
          expect(screen.getByText('invoices.notFound')).toBeInTheDocument()
      })
  })

  it('shows verify/reject buttons for PENDING invoice', async () => {
      // Mock PENDING invoice
      server.use(
        http.get('http://localhost:3000/api/v1/invoices/:id', () => {
          return HttpResponse.json({
            id: 1,
            invoiceNumber: 'INV-001',
            supplierName: 'Tech Corp', 
            invoiceDate: '2024-02-15',
            totalAmount: 1500.00,
            currency: 'TRY',
            status: 'PENDING',
            items: []
          })
        })
      )

      render(<InvoiceDetailPage params={params} />)
      
      await waitFor(() => {
          expect(screen.getByRole('button', { name: /actions.verify/i })).toBeInTheDocument()
          expect(screen.getByRole('button', { name: /actions.reject/i })).toBeInTheDocument()
      })
  })
})
