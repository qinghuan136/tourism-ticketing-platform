import request from '../utils/request'
import type { ApiResponse, PageResult } from '../types/http'
import type { TicketType, TicketTypePageQuery, TicketTypeWriteRequest } from '../types/ticketType'

const ticketTypePath = '/operator/ticket-types'

export async function getTicketTypePage(query: TicketTypePageQuery): Promise<PageResult<TicketType>> {
  const response = await request.get<ApiResponse<PageResult<TicketType>>>(ticketTypePath, { params: query })
  return response.data.data
}

export async function createTicketType(payload: TicketTypeWriteRequest): Promise<void> {
  await request.post<ApiResponse<number>>(ticketTypePath, payload)
}

export async function updateTicketType(id: number, payload: TicketTypeWriteRequest): Promise<void> {
  await request.put<ApiResponse<null>>(`${ticketTypePath}/${id}`, payload)
}

export async function deleteTicketType(id: number): Promise<void> {
  await request.delete<ApiResponse<null>>(ticketTypePath, { params: { ids: id } })
}
