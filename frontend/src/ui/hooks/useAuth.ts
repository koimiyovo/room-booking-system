import { useCallback } from 'react'
import { useAuthContext } from '@/ui/context/AuthContext'
import { AuthService } from '@/domain/service/AuthService'
import { AuthApiAdapter } from '@/infrastructure/api/AuthApiAdapter'
import type { Credentials, Registration } from '@/domain/port/primary/AuthUseCase'

const authService = new AuthService(new AuthApiAdapter())

export function useAuth() {
  const { user, isAuthenticated, isAdmin, setAuth, logout } = useAuthContext()

  const login = useCallback(
    async (credentials: Credentials) => {
      const result = await authService.login(credentials)
      setAuth(result.user, result.token)
    },
    [setAuth],
  )

  const register = useCallback(async (registration: Registration) => {
    return authService.register(registration)
  }, [])

  return { user, isAuthenticated, isAdmin, login, register, logout }
}
