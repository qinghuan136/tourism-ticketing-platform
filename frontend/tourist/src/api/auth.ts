import request from '../utils/request'
import type { LoginCredentials, RegisterPayload } from '../types/auth'
import type { ApiResponse } from '../types/http'

export async function loginAccount(credentials: LoginCredentials): Promise<string> {
  const response = await request.post<ApiResponse<string>>('/auth/login', credentials)
  if (response.data.code !== 'SUCCESS') throw new Error(response.data.message)
  return response.data.data
}

export async function registerTourist(payload: RegisterPayload): Promise<void> {
  const response = await request.post<ApiResponse<null>>('/auth/register', payload)
  if (response.data.code !== 'SUCCESS') throw new Error(response.data.message)
}
