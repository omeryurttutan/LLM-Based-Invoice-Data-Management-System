import { render, screen, fireEvent, waitFor } from '@/test-utils/render'
import { FilterPanel } from './filter-panel'

// Mock the hooks
const mockSetFilters = jest.fn()
const mockClearFilters = jest.fn()

jest.mock('@/hooks/use-invoice-filters', () => ({
  useInvoiceFilters: () => ({
    filters: {
        status: 'PENDING',
        dateFrom: '2024-01-01'
    },
    setFilters: mockSetFilters,
    clearFilters: mockClearFilters,
    activeFilterCount: 2
  })
}))

// Mock useFilterOptions
jest.mock('@/hooks/use-filter-options', () => ({
  useFilterOptions: () => ({
    data: {
        statuses: ['PENDING', 'VERIFIED'],
        categories: [{ id: 1, name: 'Tech' }]
    }
  })
}))

describe('FilterPanel', () => {
  it('renders filter panel trigger correctly', () => {
    render(<FilterPanel />)
    
    expect(screen.getByText('invoices.filters.title')).toBeInTheDocument()
    // Badge should show valid count
    expect(screen.getByText('2')).toBeInTheDocument()
  })

  it('opens panel and shows filters', async () => {
    render(<FilterPanel />)
    
    const trigger = screen.getByRole('button', { name: /invoices.filters.title/i })
    fireEvent.click(trigger)
    
    await waitFor(() => {
        expect(screen.getByText('invoices.filters.detailedOptions')).toBeInTheDocument()
        expect(screen.getByText('invoices.filters.clear')).toBeInTheDocument()
        expect(screen.getByText('invoices.filters.apply')).toBeInTheDocument()
    })
  })

  it('clear button calls clearFilters', async () => {
    render(<FilterPanel />)
    
    // Open panel
    const trigger = screen.getByRole('button', { name: /invoices.filters.title/i })
    fireEvent.click(trigger)
    
    const clearBtn = screen.getByText('invoices.filters.clear')
    fireEvent.click(clearBtn)
    
    expect(mockClearFilters).toHaveBeenCalled()
  })

  it('apply button calls setFilters', async () => {
    render(<FilterPanel />)
    
    // Open panel
    const trigger = screen.getByRole('button', { name: /invoices.filters.title/i })
    fireEvent.click(trigger)
    
    const applyBtn = screen.getByText('invoices.filters.apply')
    fireEvent.click(applyBtn)
    
    expect(mockSetFilters).toHaveBeenCalled()
  })
})
