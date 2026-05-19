export type UserRoleDto = 'ADMIN' | 'USER'
export type UserStatusDto = 'CREATED' | 'ACTIVE' | 'INACTIVE' | 'DELETED'

export interface UserStatusInfoDto {
  status: UserStatusDto
  since: string
  reason: string | null
}

export interface UserDto {
  id: string
  name: string
  email: string
  role: UserRoleDto
  registered_at: string
  status_info: UserStatusInfoDto
}

export interface LoginRequestDto {
  email: string
  password: string
}

export interface LoginResponseDto {
  token: string
}

export interface RegisterRequestDto {
  name: string
  email: string
  password: string
}

export interface RegisterResponseDto {
  id: string
  name: string
  email: string
  registered_at: string
}
