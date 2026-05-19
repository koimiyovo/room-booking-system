import { http } from '@/infrastructure/api/http'
import { TokenStorage } from '@/infrastructure/auth/TokenStorage'
import type { LoginRequestDto, LoginResponseDto, RegisterRequestDto } from '@/infrastructure/api/dto/UserDto'
import type { User } from '@/domain/model/User'
import type { Credentials, Registration } from '@/domain/port/primary/AuthUseCase'
import type { AuthRepository } from '@/domain/port/secondary/AuthRepository'

function parseTokenPayload(token: string): User {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return {
      id: payload.sub ?? '',
      name: payload.name ?? '',
      email: payload.email ?? payload.sub ?? '',
      role: payload.role ?? 'USER',
      registeredAt: new Date().toISOString(),
      statusInfo: { status: 'ACTIVE', since: new Date().toISOString(), reason: null },
    }
  } catch {
    return {
      id: '',
      name: '',
      email: '',
      role: 'USER',
      registeredAt: new Date().toISOString(),
      statusInfo: { status: 'ACTIVE', since: new Date().toISOString(), reason: null },
    }
  }
}

export class AuthApiAdapter implements AuthRepository {
  async login(credentials: Credentials): Promise<{ user: User; token: string }> {
    const body: LoginRequestDto = { email: credentials.email, password: credentials.password }
    const response = await http.post<LoginResponseDto>('/auth/login', body)
    TokenStorage.save(response.token)
    const user = parseTokenPayload(response.token)
    return { user, token: response.token }
  }

  async register(registration: Registration): Promise<User> {
    const body: RegisterRequestDto = {
      name: registration.name,
      email: registration.email,
      password: registration.password,
    }
    await http.post('/auth/register', body)
    const result = await this.login({ email: registration.email, password: registration.password })
    return result.user
  }
}
