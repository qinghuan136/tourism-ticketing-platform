export type AccountRole = 'TOURIST' | 'OPERATOR' | 'STAFF' | 'ADMIN'

export interface LoginCredentials {
  loginName: string
  password: string
}

export interface RegisterForm {
  loginName: string
  password: string
  confirmPassword: string
  displayName: string
  phone: string
}

export type RegisterPayload = Omit<RegisterForm, 'confirmPassword'>

export interface TouristIdentity {
  userId: number
  loginName: string
  roleCode: 'TOURIST'
  expiresAt: number
}
