import { render, screen, fireEvent } from '@/test-utils/render'
import { ConfirmDialog } from './confirm-dialog'

describe('ConfirmDialog', () => {
  const mockOnConfirm = jest.fn()
  const mockOnCancel = jest.fn() // if prop exists or we check open state change

  const defaultProps = {
    open: true,
    onOpenChange: jest.fn(),
    title: 'Confirm Action',
    description: 'Are you sure?',
    onConfirm: mockOnConfirm,
  }

  it('renders dialog content when open', () => {
    render(
      <ConfirmDialog {...defaultProps}>
        <button>Trigger</button>
      </ConfirmDialog>
    )
    
    expect(screen.getByText('Confirm Action')).toBeInTheDocument()
    expect(screen.getByText('Are you sure?')).toBeInTheDocument()
  })

  it('calls onConfirm when confirmed', () => {
    render(
      <ConfirmDialog {...defaultProps}>
        <button>Trigger</button>
      </ConfirmDialog>
    )
    
    // Assuming default button text is 'Confirm' or similar, or checking variant
    // Usually "Continue" or "Confirm"
    // Using simple locator or by role
    const confirmBtn = screen.getByRole('button', { name: /confirm|continue|evet/i })
    fireEvent.click(confirmBtn)
    
    expect(mockOnConfirm).toHaveBeenCalled()
  })
})
