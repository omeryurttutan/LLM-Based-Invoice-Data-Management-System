import { render, screen, fireEvent, waitFor } from '@/test-utils/render'
import { NotificationDropdown } from './notification-dropdown'
import { server } from '@/mocks/server'
import { http, HttpResponse } from 'msw'

describe('NotificationDropdown', () => {
  it('renders notification bell', () => {
    render(<NotificationDropdown />)
    
    // Check for bell icon or trigger button
    // The trigger usually has a label or icon
    // We can assume it has anaria-label or we find by role button
    const trigger = screen.getByRole('button')
    expect(trigger).toBeInTheDocument()
  })

  it('shows badge count', async () => {
    render(<NotificationDropdown />)
    
    await waitFor(() => {
        // MSW returns count: 5
        expect(screen.getByText('5')).toBeInTheDocument()
    })
  })

  it('opens dropdown and shows notifications', async () => {
    render(<NotificationDropdown />)
    
    const trigger = screen.getByRole('button')
    fireEvent.click(trigger)
    
    await waitFor(() => {
        // MSW returns a notification with 'New Invoice'
        expect(screen.getByText('New Invoice')).toBeInTheDocument()
        expect(screen.getByText('Invoice #123 uploaded')).toBeInTheDocument()
    })
  })
})
