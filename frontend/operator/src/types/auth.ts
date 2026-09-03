export type AccountRole = 'TOURIST' | 'OPERATOR' | 'STAFF' | 'ADMIN'
export type OperatorAppRole = Extract<AccountRole, 'OPERATOR' | 'STAFF'>

export interface LoginCredentials {
  loginName: string
  password: string
}

export interface OperatorIdentity {
  userId: number
  loginName: string
  roleCode: OperatorAppRole
  venueId: number | null
  expiresAt: number
}
