import request from '../utils/request'
import type { ApiResponse, PageResult } from '../types/http'
import type { Ticket, TicketQuery } from '../types/ticket'

function unwrap<T>(response: ApiResponse<T>): T {
  if (response.code !== 'SUCCESS') throw new Error(response.message)
  return response.data
}

export async function pageMyTickets(query: TicketQuery): Promise<PageResult<Ticket>> {
  const response = await request.get<ApiResponse<PageResult<Ticket>>>('/tourist/tickets', { params: query })
  return unwrap(response.data)
}

export async function getMyTicket(ticketId: number): Promise<Ticket> {
  const response = await request.get<ApiResponse<Ticket>>(`/tourist/tickets/${ticketId}`)
  return unwrap(response.data)
}
