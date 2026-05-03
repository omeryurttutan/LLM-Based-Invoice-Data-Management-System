import { render, screen, fireEvent, waitFor } from '@/test-utils/render'
import LoginPage from './page'
import { server } from '@/mocks/server'
import { http, HttpResponse } from 'msw'

// Mock useSearchParams specifically for this test file if needed per test
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

describe('LoginPage', () => {
  it('renders login form correctly', () => {
    render(<LoginPage />)
    
    expect(screen.getByLabelText(/auth.login.emailLabel/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/auth.login.passwordLabel/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /auth.login.submit/i })).toBeInTheDocument()
  })

  it('shows validation errors for empty submission', async () => {
    render(<LoginPage />)
    
    fireEvent.click(screen.getByRole('button', { name: /auth.login.submit/i }))
    
    await waitFor(() => {
      expect(screen.getByText(/auth.validation.required/i)).toBeInTheDocument()
    })
  })

  it('shows validation error for invalid email', async () => {
    render(<LoginPage />)
    
    fireEvent.change(screen.getByLabelText(/auth.login.emailLabel/i), { target: { value: 'invalid-email' } })
    fireEvent.click(screen.getByRole('button', { name: /auth.login.submit/i }))
    
    await waitFor(() => {
      expect(screen.getByText(/auth.validation.email/i)).toBeInTheDocument()
    })
  })

  it('submits form with valid credentials', async () => {
    render(<LoginPage />)
    
    fireEvent.change(screen.getByLabelText(/auth.login.emailLabel/i), { target: { value: 'test@example.com' } })
    fireEvent.change(screen.getByLabelText(/auth.login.passwordLabel/i), { target: { value: 'Password123!' } })
    
    fireEvent.click(screen.getByRole('button', { name: /auth.login.submit/i }))
    
    await waitFor(() => {
      expect(mockPush).toHaveBeenCalled()
    })
  })

  it('shows error message on invalid credentials', async () => {
    // Override handler to return 401
    server.use(
      http.post('http://localhost:3000/api/v1/auth/login', () => {
        return HttpResponse.json({ message: 'Invalid credentials' }, { status: 401 })
      })
    )

    render(<LoginPage />)
    
    fireEvent.change(screen.getByLabelText(/auth.login.emailLabel/i), { target: { value: 'wrong@example.com' } })
    fireEvent.change(screen.getByLabelText(/auth.login.passwordLabel/i), { target: { value: 'WrongPass' } })
    
    fireEvent.click(screen.getByRole('button', { name: /auth.login.submit/i }))
    
    await waitFor(() => {
      expect(screen.getByText('Invalid credentials')).toBeInTheDocument()
    })
  })
})
