export type UserRole = 'ADMIN' | 'USER'
export type UserStatus = 'CREATED' | 'ACTIVE' | 'INACTIVE' | 'DELETED'

export type UserStatusInfo = {
    status: UserStatus
    since: string
    reason: string | null
}

export type User = {
    id: string
    name: string
    email: string
    role: UserRole
    registeredAt: string
    statusInfo: UserStatusInfo
}
