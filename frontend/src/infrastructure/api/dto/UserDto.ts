export type UserRoleDto = 'ADMIN' | 'USER'
export type UserStatusDto = 'CREATED' | 'ACTIVE' | 'INACTIVE' | 'DELETED'

export type UserStatusInfoDto = {
    status: UserStatusDto
    since: string
    reason: string | null
}

export type UserDto = {
    id: string
    name: string
    email: string
    role: UserRoleDto
    registered_at: string
    status_info: UserStatusInfoDto
}

export type LoginRequestDto = {
    email: string
    password: string
}

export type LoginResponseDto = {
    token: string
}

export type RegisterRequestDto = {
    name: string
    email: string
    password: string
}

export type RegisterResponseDto = {
    id: string
    name: string
    email: string
    registered_at: string
}
