import { createContext, useContext, useState, useCallback, type ReactNode } from 'react'
import type { User } from '@/domain/model/User'
import { TokenStorage } from '@/infrastructure/auth/TokenStorage'

interface AuthState {
  user: User | null
  token: string | null
}

interface AuthContextValue extends AuthState {
  setAuth: (user: User, token: string) => void
  logout: () => void
  isAuthenticated: boolean
  isAdmin: boolean
}

const AuthContext = createContext<AuthContextValue | null>(null)

function parseStoredToken(token: string): User | null {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return {
      id: payload.sub ?? '',
      name: payload.name ?? '',
      email: payload.email ?? payload.sub ?? '',
      role: payload.role ?? 'USER',
      registeredAt: '',
      statusInfo: { status: 'ACTIVE', since: '', reason: null },
    }
  } catch {
    return null
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>(() => {
    const token = TokenStorage.get()
    if (!token) return { user: null, token: null }
    const user = parseStoredToken(token)
    if (!user) {
      TokenStorage.clear()
      return { user: null, token: null }
    }
    return { user, token }
  })

  const setAuth = useCallback((user: User, token: string) => {
    setState({ user, token })
  }, [])

  const logout = useCallback(() => {
    TokenStorage.clear()
    setState({ user: null, token: null })
  }, [])

  return (
    <AuthContext.Provider
      value={{
        ...state,
        setAuth,
        logout,
        isAuthenticated: state.token !== null,
        isAdmin: state.user?.role === 'ADMIN',
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuthContext(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuthContext must be used inside AuthProvider')
  return ctx
}
