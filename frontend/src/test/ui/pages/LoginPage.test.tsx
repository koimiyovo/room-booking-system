import { describe, it, expect, vi, beforeAll, afterAll, afterEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'
import LoginPage from '@/ui/pages/LoginPage'
import { AuthProvider } from '@/ui/context/AuthContext'

const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return { ...(actual as object), useNavigate: () => mockNavigate }
})

const BASE = 'http://localhost/api/v1'

// Minimal valid JWT: header.payload.signature (payload is base64 of JSON)
const payload = btoa(JSON.stringify({ sub: 'user-1', email: 'alice@example.com', role: 'USER', name: 'Alice' }))
const mockToken = `eyJhbGciOiJIUzI1NiJ9.${payload}.sig`

const server = setupServer(
  http.post(`${BASE}/auth/login`, () => HttpResponse.json({ token: mockToken })),
)

beforeAll(() => server.listen())
afterEach(() => { server.resetHandlers(); mockNavigate.mockReset() })
afterAll(() => server.close())

function renderLoginPage() {
  return render(
    <MemoryRouter>
      <AuthProvider>
        <LoginPage />
      </AuthProvider>
    </MemoryRouter>,
  )
}

describe('LoginPage', () => {
  it('renders email and password fields', () => {
    renderLoginPage()
    expect(screen.getByLabelText(/email/i)).toBeDefined()
    expect(screen.getByLabelText(/password/i)).toBeDefined()
  })

  it('navigates to /rooms on successful login', async () => {
    renderLoginPage()
    fireEvent.change(screen.getByLabelText(/email/i), { target: { value: 'alice@example.com' } })
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: 'secret' } })
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }))
    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/rooms'))
  })

  it('displays an error message when login fails', async () => {
    server.use(
      http.post(`${BASE}/auth/login`, () => HttpResponse.text('Invalid credentials', { status: 401 })),
    )
    renderLoginPage()
    fireEvent.change(screen.getByLabelText(/email/i), { target: { value: 'alice@example.com' } })
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: 'wrong' } })
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }))
    await waitFor(() => expect(screen.getByText(/invalid credentials/i)).toBeDefined())
  })
})
