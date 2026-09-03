import request from '../utils/request'
import type { Visitor, VisitorCreatePayload, VisitorStatus, VisitorUpdatePayload } from '../types/visitor'
import type { ApiResponse } from '../types/http'

function ensureSuccess(response: ApiResponse<unknown>): void {
  if (response.code !== 'SUCCESS') throw new Error(response.message)
}

export async function listVisitors(status?: VisitorStatus): Promise<Visitor[]> {
  const response = await request.get<ApiResponse<Visitor[]>>('/tourist/visitors', { params: { status } })
  ensureSuccess(response.data)
  return response.data.data
}

export async function createVisitor(payload: VisitorCreatePayload): Promise<number> {
  const response = await request.post<ApiResponse<number>>('/tourist/visitors', payload)
  ensureSuccess(response.data)
  return response.data.data
}

export async function updateVisitor(visitorId: number, payload: VisitorUpdatePayload): Promise<void> {
  const response = await request.put<ApiResponse<null>>(`/tourist/visitors/${visitorId}`, payload)
  ensureSuccess(response.data)
}

export async function updateVisitorStatus(visitorId: number, status: VisitorStatus): Promise<void> {
  const response = await request.patch<ApiResponse<null>>(`/tourist/visitors/${visitorId}/status`, { status })
  ensureSuccess(response.data)
}
