import { render, screen, fireEvent } from '@/test-utils/render'
import { DataTablePagination } from './data-table-pagination'

describe('DataTablePagination', () => {
  const mockOnPageChange = jest.fn()
  const mockOnPageSizeChange = jest.fn()

  const defaultProps = {
    pageIndex: 0,
    pageSize: 10,
    totalElements: 50,
    totalPages: 5,
    onPageChange: mockOnPageChange,
    onPageSizeChange: mockOnPageSizeChange
  }

  it('renders pagination info correctly', () => {
    render(<DataTablePagination {...defaultProps} />)
    
    expect(screen.getByText('Page 1 of 5')).toBeInTheDocument()
    expect(screen.getByText('50 total rows')).toBeInTheDocument() // Adjust text based on actual implementation
  })

  it('disables previous button on first page', () => {
    render(<DataTablePagination {...defaultProps} pageIndex={0} />)
    
    const prevBtn = screen.getByRole('button', { name: /Go to previous page/i })
    expect(prevBtn).toBeDisabled()
  })

  it('calls onPageChange when next button clicked', () => {
    render(<DataTablePagination {...defaultProps} pageIndex={0} />)
    
    const nextBtn = screen.getByRole('button', { name: /Go to next page/i })
    fireEvent.click(nextBtn)
    
    expect(mockOnPageChange).toHaveBeenCalledWith(1)
  })

  it('disables next button on last page', () => {
    render(<DataTablePagination {...defaultProps} pageIndex={4} />)
    
    const nextBtn = screen.getByRole('button', { name: /Go to next page/i })
    expect(nextBtn).toBeDisabled()
  })
})
