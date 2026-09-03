import request from '../utils/request'
import type { LoginCredentials } from '../types/auth'
import type { ApiResponse } from '../types/http'

/** 使用账号密码换取后端签发的 JWT。 */
export async function loginAccount(credentials: LoginCredentials): Promise<string> {
  const response = await request.post<ApiResponse<string>>('/auth/login', credentials)

  if (response.data.code !== 'SUCCESS') {
    throw new Error(response.data.message)
  }

  return response.data.data
}
