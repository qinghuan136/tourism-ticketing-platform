export type AccountStatus = 'ACTIVE' | 'DISABLED'
export type StaffStatusFilter = '' | AccountStatus

/** 与后端 UserAccountVO 保持一致。 */
export interface StaffAccount {
  id: number
  loginName: string
  displayName: string
  phone: string
  roleCode: 'STAFF'
  venueId: number
  status: AccountStatus
  createdAt: string
}

/** GET /operator/staff/page 的查询参数。 */
export interface StaffPageQuery {
  page: number
  pageSize: number
  keyword?: string
  status?: AccountStatus
}

/** POST /operator/staff 的请求体。 */
export interface StaffCreateRequest {
  loginName: string
  password: string
  displayName: string
  phone: string
}

/** PUT /operator/staff/{staffId} 的请求体。 */
export interface StaffUpdateRequest {
  displayName: string
  phone: string
}

/** PUT /operator/staff/{staffId}/password 的请求体。 */
export interface StaffPasswordResetRequest {
  password: string
}
