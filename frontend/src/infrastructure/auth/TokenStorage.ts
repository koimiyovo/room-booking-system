const KEY = 'auth_token'

export const TokenStorage = {
  get: (): string | null => localStorage.getItem(KEY),
  save: (token: string): void => localStorage.setItem(KEY, token),
  clear: (): void => localStorage.removeItem(KEY),
}
