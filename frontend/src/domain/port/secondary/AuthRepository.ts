import type { User } from '@/domain/model/User'
import type { Credentials, Registration } from '@/domain/port/primary/AuthUseCase'

export interface AuthRepository {
  login(credentials: Credentials): Promise<{ user: User; token: string }>
  register(registration: Registration): Promise<User>
}
