export type SessionStatus = 'DRAFT' | 'OPEN' | 'CLOSED' | 'ENDED' | 'CANCELLED'
export type SessionEvent = 'PUBLISH' | 'CLOSE_BOOKING' | 'REOPEN_BOOKING' | 'CANCEL' | 'SESSION_ENDED'

export interface SessionTicketType {
  sessionTicketTypeId: number
  ticketTypeId: number
  ticketTypeName: string
  salePrice: number
  allocatedQuantity: number
  remainingQuantity: number
  status: 'ON_SALE' | 'OFF_SALE'
}

export interface AdmissionSession {
  id: number
  visitDate: string
  startTime: string
  endTime: string
  bookingStartAt: string
  bookingEndAt: string
  totalCapacity: number
  remainingCapacity: number
  status: SessionStatus
  ticketTypes: SessionTicketType[]
  createdAt: string
  updatedAt: string
}

export interface SessionTicketTypeConfig {
  ticketTypeId: number
  salePrice: number
  allocatedQuantity: number
}

export interface SessionWriteRequest {
  visitDate: string
  startTime: string
  endTime: string
  bookingStartAt: string
  bookingEndAt: string
  totalCapacity: number
  ticketTypes: SessionTicketTypeConfig[]
}

export interface SessionPageQuery {
  visitDate?: string
  status?: SessionStatus
  page: number
  size: number
}
