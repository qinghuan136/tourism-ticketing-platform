import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { loginAccount } from '../api/auth'
import type { AccountRole, LoginCredentials, OperatorIdentity } from '../types/auth'
import { accessTokenKey } from '../utils/request'

interface JwtPayload {
  sub: string
  loginName: string
  roleCode: AccountRole
  venueId?: number | string | null
  exp: number
}

interface StoredSession {
  token: string
  identity: OperatorIdentity
}

function decodeJwtPayload(token: string): JwtPayload {
  const segments = token.split('.')
  if (segments.length !== 3 || !segments[1]) {
    throw new Error('登录凭证格式无效')
  }

  const base64 = segments[1].replace(/-/g, '+').replace(/_/g, '/')
  const padded = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=')
  const bytes = Uint8Array.from(atob(padded), (character) => character.charCodeAt(0))
  const value: unknown = JSON.parse(new TextDecoder().decode(bytes))

  if (typeof value !== 'object' || value === null) {
    throw new Error('登录凭证内容无效')
  }

  const payload = value as Record<string, unknown>
  if (
    typeof payload.sub !== 'string'
    || typeof payload.loginName !== 'string'
    || typeof payload.roleCode !== 'string'
    || typeof payload.exp !== 'number'
  ) {
    throw new Error('登录凭证缺少必要身份信息')
  }

  return payload as unknown as JwtPayload
}

function createOperatorIdentity(token: string): OperatorIdentity {
  const payload = decodeJwtPayload(token)
  if (payload.roleCode !== 'OPERATOR' && payload.roleCode !== 'STAFF') {
    throw new Error('当前账号无权登录运营端')
  }

  const userId = Number(payload.sub)
  const venueId = payload.venueId == null ? null : Number(payload.venueId)
  const expiresAt = payload.exp * 1000

  if (!Number.isSafeInteger(userId) || userId <= 0) {
    throw new Error('登录凭证中的用户信息无效')
  }
  if (venueId !== null && (!Number.isSafeInteger(venueId) || venueId <= 0)) {
    throw new Error('登录凭证中的景点信息无效')
  }
  if (!Number.isFinite(expiresAt) || expiresAt <= Date.now()) {
    throw new Error('登录凭证已过期，请重新登录')
  }

  return {
    userId,
    loginName: payload.loginName,
    roleCode: payload.roleCode,
    venueId,
    expiresAt,
  }
}

function restoreSession(): StoredSession | null {
  const token = localStorage.getItem(accessTokenKey)
  if (!token) return null

  try {
    return { token, identity: createOperatorIdentity(token) }
  } catch {
    localStorage.removeItem(accessTokenKey)
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const restoredSession = restoreSession()
  const accessToken = ref<string | null>(restoredSession?.token ?? null)
  const identity = ref<OperatorIdentity | null>(restoredSession?.identity ?? null)
  const isAuthenticated = computed(() => accessToken.value !== null && identity.value !== null)

  async function login(credentials: LoginCredentials): Promise<void> {
    const token = await loginAccount(credentials)
    const operatorIdentity = createOperatorIdentity(token)

    localStorage.setItem(accessTokenKey, token)
    accessToken.value = token
    identity.value = operatorIdentity
  }

  function logout(): void {
    localStorage.removeItem(accessTokenKey)
    accessToken.value = null
    identity.value = null
  }

  function hasValidSession(): boolean {
    if (!identity.value || !accessToken.value || identity.value.expiresAt <= Date.now()) {
      logout()
      return false
    }
    return true
  }

  return {
    accessToken,
    identity,
    isAuthenticated,
    login,
    logout,
    hasValidSession,
  }
})
