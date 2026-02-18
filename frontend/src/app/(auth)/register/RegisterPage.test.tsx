import { render, screen, fireEvent, waitFor } from '@/test-utils/render'
import RegisterPage from './page'
import { server } from '@/mocks/server'
import { http, HttpResponse } from 'msw'

const mockPush = jest.fn()
jest.mock('next/navigation', () => ({
  useRouter: () => ({
    push: mockPush,
    refresh: jest.fn(),
  }),
}))

describe('RegisterPage', () => {
  it('renders register form correctly', () => {
    render(<RegisterPage />)
    
    expect(screen.getByLabelText(/auth.register.fullNameLabel/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/auth.register.emailLabel/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/auth.register.passwordLabel/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/auth.register.confirmPasswordLabel/i)).toBeInTheDocument()
  })

  it('shows validation errors for empty submission', async () => {
    render(<RegisterPage />)
    
    fireEvent.click(screen.getByRole('button', { name: /auth.register.submit/i }))
    
    await waitFor(() => {
      expect(screen.getAllByText(/auth.validation.required/i).length).toBeGreaterThan(0)
    })
  })

  it('validates password requirements', async () => {
    render(<RegisterPage />)
    
    const passwordInput = screen.getByLabelText(/auth.register.passwordLabel/i)
    fireEvent.change(passwordInput, { target: { value: 'weak' } })
    
    // Check if requirements list is visible
    expect(screen.getByText(/auth.validation.requirements.title/i)).toBeInTheDocument()
    
    // Submit to trigger validation error text
    fireEvent.click(screen.getByRole('button', { name: /auth.register.submit/i }))
    
    await waitFor(() => {
      expect(screen.getByText(/auth.validation.minLength/i)).toBeInTheDocument()
    })
  })

  it('validates password confirmation match', async () => {
    render(<RegisterPage />)
    
    fireEvent.change(screen.getByLabelText(/auth.register.passwordLabel/i), { target: { value: 'Password123!' } })
    fireEvent.change(screen.getByLabelText(/auth.register.confirmPasswordLabel/i), { target: { value: 'Password123' } }) // Missing !
    
    fireEvent.click(screen.getByRole('button', { name: /auth.register.submit/i }))
    
    await waitFor(() => {
      expect(screen.getByText(/auth.validation.passwordMismatch/i)).toBeInTheDocument()
    })
  })

  it('submits form with valid data', async () => {
    render(<RegisterPage />)
    
    fireEvent.change(screen.getByLabelText(/auth.register.fullNameLabel/i), { target: { value: 'Test User' } })
    fireEvent.change(screen.getByLabelText(/auth.register.emailLabel/i), { target: { value: 'new@example.com' } })
    fireEvent.change(screen.getByLabelText(/auth.register.passwordLabel/i), { target: { value: 'Password123!' } })
    fireEvent.change(screen.getByLabelText(/auth.register.confirmPasswordLabel/i), { target: { value: 'Password123!' } })
    
    fireEvent.click(screen.getByRole('button', { name: /auth.register.submit/i }))
    
    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith('/')
    })
  })

  it('shows error for duplicate email', async () => {
    server.use(
      http.post('http://localhost:3000/api/v1/auth/register', () => {
        return HttpResponse.json({ message: 'Email already exists' }, { status: 409 })
      })
    )

    render(<RegisterPage />)
    
    fireEvent.change(screen.getByLabelText(/auth.register.fullNameLabel/i), { target: { value: 'Test User' } })
    fireEvent.change(screen.getByLabelText(/auth.register.emailLabel/i), { target: { value: 'existing@example.com' } })
    fireEvent.change(screen.getByLabelText(/auth.register.passwordLabel/i), { target: { value: 'Password123!' } })
    fireEvent.change(screen.getByLabelText(/auth.register.confirmPasswordLabel/i), { target: { value: 'Password123!' } })
    
    fireEvent.click(screen.getByRole('button', { name: /auth.register.submit/i }))
    
    await waitFor(() => {
      expect(screen.getByText('Email already exists')).toBeInTheDocument()
    })
  })
})
