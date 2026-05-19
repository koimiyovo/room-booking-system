import type {User} from '@/domain/model/User'

export type Credentials = {
    email: string
    password: string
}

export type Registration = {
    name: string
    email: string
    password: string
}

export interface AuthUseCase {
    login(credentials: Credentials): Promise<{ user: User; token: string }>

    register(registration: Registration): Promise<User>
}
