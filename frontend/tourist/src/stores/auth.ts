import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { loginAccount } from '../api/auth'
import type { AccountRole, LoginCredentials, TouristIdentity } from '../types/auth'
import { accessTokenKey } from '../utils/request'

interface JwtPayload {
  sub: string
  loginName: string
  roleCode: AccountRole
  exp: number
}

function decodePayload(token: string): JwtPayload {
  const segment = token.split('.')[1]
  if (!segment) throw new Error('登录凭证格式无效')

  const base64 = segment.replace(/-/g, '+').replace(/_/g, '/')
  const padded = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=')
  const value: unknown = JSON.parse(
    new TextDecoder().decode(Uint8Array.from(atob(padded), (char) => char.charCodeAt(0))),
  )

  if (typeof value !== 'object' || value === null) throw new Error('登录凭证内容无效')
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

function createIdentity(token: string): TouristIdentity {
  const payload = decodePayload(token)
  const userId = Number(payload.sub)
  const expiresAt = payload.exp * 1000

  if (payload.roleCode !== 'TOURIST') throw new Error('当前账号不是游客账号')
  if (!Number.isSafeInteger(userId) || userId <= 0) throw new Error('登录凭证中的用户信息无效')
  if (!Number.isFinite(expiresAt) || expiresAt <= Date.now()) throw new Error('登录凭证已过期')

  return {
    userId,
    loginName: payload.loginName,
    roleCode: 'TOURIST',
    expiresAt,
  }
}

function restoreSession(): { token: string; identity: TouristIdentity } | null {
  const token = localStorage.getItem(accessTokenKey)
  if (!token) return null

  try {
    return { token, identity: createIdentity(token) }
  } catch {
    localStorage.removeItem(accessTokenKey)
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const restored = restoreSession()
  const accessToken = ref<string | null>(restored?.token ?? null)
  const identity = ref<TouristIdentity | null>(restored?.identity ?? null)
  const isAuthenticated = computed(() => accessToken.value !== null && identity.value !== null)

  async function login(credentials: LoginCredentials): Promise<void> {
    const token = await loginAccount(credentials)
    const touristIdentity = createIdentity(token)
    localStorage.setItem(accessTokenKey, token)
    accessToken.value = token
    identity.value = touristIdentity
  }

  function logout(): void {
    localStorage.removeItem(accessTokenKey)
    accessToken.value = null
    identity.value = null
  }

  function hasValidSession(): boolean {
    if (!accessToken.value || !identity.value || identity.value.expiresAt <= Date.now()) {
      logout()
      return false
    }
    return true
  }

  return { accessToken, identity, isAuthenticated, login, logout, hasValidSession }
})
