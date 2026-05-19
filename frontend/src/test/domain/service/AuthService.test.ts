import { describe, it, expect, vi, beforeEach } from 'vitest'
import { AuthService } from '@/domain/service/AuthService'
import type { AuthRepository } from '@/domain/port/secondary/AuthRepository'
import type { User } from '@/domain/model/User'

const mockUser: User = {
  id: 'user-1',
  name: 'Alice',
  email: 'alice@example.com',
  role: 'USER',
  registeredAt: '2024-01-01T00:00:00Z',
  statusInfo: { status: 'ACTIVE', since: '2024-01-01T00:00:00Z', reason: null },
}

describe('AuthService', () => {
  let repository: AuthRepository
  let service: AuthService

  beforeEach(() => {
    repository = {
      login: vi.fn().mockResolvedValue({ user: mockUser, token: 'mock-token' }),
      register: vi.fn().mockResolvedValue(mockUser),
    }
    service = new AuthService(repository)
  })

  it('delegates login to the repository and returns user and token', async () => {
    const result = await service.login({ email: 'alice@example.com', password: 'secret' })
    expect(repository.login).toHaveBeenCalledWith({ email: 'alice@example.com', password: 'secret' })
    expect(result.user).toEqual(mockUser)
    expect(result.token).toBe('mock-token')
  })

  it('delegates register to the repository and returns the created user', async () => {
    const user = await service.register({ name: 'Alice', email: 'alice@example.com', password: 'secret' })
    expect(repository.register).toHaveBeenCalledOnce()
    expect(user).toEqual(mockUser)
  })
})
