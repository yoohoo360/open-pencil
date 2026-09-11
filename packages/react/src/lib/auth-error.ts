export const AUTH_ERROR_CODE = {
  UNAUTHORIZED: 40100,
  TOKEN_EXPIRED: 40101,
  INVALID_TOKEN: 40102
} as const

export type AuthErrorCode = (typeof AUTH_ERROR_CODE)[keyof typeof AUTH_ERROR_CODE]

export function readAuthErrorCode(data: unknown): number | null {
  if (!data || typeof data !== 'object') return null
  const code = (data as { code?: unknown }).code
  if (typeof code === 'number' && Number.isFinite(code)) return code
  if (typeof code === 'string' && code.trim() !== '') {
    const parsed = Number(code)
    if (Number.isFinite(parsed)) return parsed
  }
  return null
}

export function shouldAttemptTokenRefresh(
  status: number | undefined,
  code: number | null
): boolean {
  if (code === AUTH_ERROR_CODE.TOKEN_EXPIRED) return true
  if (code === AUTH_ERROR_CODE.INVALID_TOKEN) return false
  return status === 401 || status === 403
}
