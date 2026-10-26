import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'

const { useAuthMock } = vi.hoisted(() => ({ useAuthMock: vi.fn() }))
vi.mock('../context/AuthContext.jsx', () => ({ useAuth: useAuthMock }))

import LoginPage from '../pages/LoginPage.jsx'
import Sidebar from '../components/Sidebar.jsx'

afterEach(() => {
  cleanup()
  vi.clearAllMocks()
})

describe('login screen', () => {
  it('requires username and password before submission', () => {
    const login = vi.fn()
    useAuthMock.mockReturnValue({ login, isAuthenticated: false, ready: true })
    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    )

    const username = screen.getByLabelText('Username')
    const password = screen.getByLabelText('Password')
    expect(username.required).toBe(true)
    expect(password.required).toBe(true)
    expect(password.getAttribute('type')).toBe('password')
    fireEvent.click(screen.getByRole('button', { name: 'Sign in' }))
    expect(login).not.toHaveBeenCalled()
  })

  it('shows the authentication error returned by login', async () => {
    useAuthMock.mockReturnValue({
      login: vi.fn().mockRejectedValue({ response: { data: { message: 'Invalid credentials' } } }),
      isAuthenticated: false,
      ready: true,
    })
    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    )
    fireEvent.change(screen.getByLabelText('Username'), { target: { value: 'wrong' } })
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'wrong' } })
    fireEvent.submit(screen.getByRole('button', { name: 'Sign in' }).closest('form'))

    expect((await screen.findByRole('alert')).textContent).toContain('Invalid credentials')
  })

  it('explains when the backend cannot be reached', async () => {
    useAuthMock.mockReturnValue({
      login: vi.fn().mockRejectedValue(new Error('Network Error')),
      isAuthenticated: false,
      ready: true,
    })
    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    )
    fireEvent.change(screen.getByLabelText('Username'), { target: { value: 'admin' } })
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'password' } })
    fireEvent.submit(screen.getByRole('button', { name: 'Sign in' }).closest('form'))

    expect((await screen.findByRole('alert')).textContent).toContain(
      'Cannot reach the FixFlow backend',
    )
  })
})

describe('role-based navigation', () => {
  it('shows administration links to admins and omits them for staff', () => {
    const renderSidebar = (role) => {
      useAuthMock.mockReturnValue({
        user: { username: 'demo', fullName: 'Demo User', role },
        hasRole: (...roles) => roles.includes(role),
      })
      return render(
        <MemoryRouter>
          <Sidebar />
        </MemoryRouter>,
      )
    }

    const admin = renderSidebar('ADMIN')
    expect(screen.getByRole('link', { name: /Audit trail/ })).toBeTruthy()
    expect(screen.getByRole('link', { name: /Users/ })).toBeTruthy()
    admin.unmount()

    renderSidebar('SERVICE_STAFF')
    expect(screen.queryByRole('link', { name: /Audit trail/ })).toBeNull()
    expect(screen.queryByRole('link', { name: /^Users/ })).toBeNull()
    expect(screen.getByRole('link', { name: /Inventory/ })).toBeTruthy()
  })
})
