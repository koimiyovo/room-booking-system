import type { User } from '@/domain/model/User'
import type { AuthUseCase, Credentials, Registration } from '@/domain/port/primary/AuthUseCase'
import type { AuthRepository } from '@/domain/port/secondary/AuthRepository'

export class AuthService implements AuthUseCase {
  constructor(private readonly repository: AuthRepository) {}

  login(credentials: Credentials): Promise<{ user: User; token: string }> {
    return this.repository.login(credentials)
  }

  register(registration: Registration): Promise<User> {
    return this.repository.register(registration)
  }
}
