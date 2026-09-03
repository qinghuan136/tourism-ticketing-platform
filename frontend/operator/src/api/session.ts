import request from '../utils/request'
import type { ApiResponse, PageResult } from '../types/http'
import type { AdmissionSession, SessionEvent, SessionPageQuery, SessionWriteRequest } from '../types/session'

const sessionPath = '/operator/sessions'

export async function getSessionPage(query: SessionPageQuery): Promise<PageResult<AdmissionSession>> {
  const response = await request.get<ApiResponse<PageResult<AdmissionSession>>>(sessionPath, { params: query })
  return response.data.data
}

export async function getSession(id: number): Promise<AdmissionSession> {
  const response = await request.get<ApiResponse<AdmissionSession>>(`${sessionPath}/${id}`)
  return response.data.data
}

export async function createSession(payload: SessionWriteRequest): Promise<void> {
  await request.post<ApiResponse<number>>(sessionPath, payload)
}

export async function updateSession(id: number, payload: SessionWriteRequest): Promise<void> {
  await request.put<ApiResponse<null>>(`${sessionPath}/${id}`, payload)
}

export async function sendSessionEvent(id: number, event: SessionEvent): Promise<void> {
  await request.post<ApiResponse<null>>(`${sessionPath}/${id}/events`, { event })
}

export async function deleteSession(id: number): Promise<void> {
  await request.delete<ApiResponse<null>>(`${sessionPath}/${id}`)
}
