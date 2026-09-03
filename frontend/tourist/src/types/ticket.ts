export type TicketStatus = 'VALID' | 'REFUNDING' | 'USED' | 'VOID' | 'EXPIRED'

export interface TicketQuery {
  page: number
  size: number
  status?: TicketStatus
}

export interface Ticket {
  id: number
  ticketCode: string
  status: TicketStatus
  venueName: string
  visitDate: string
  startTime: string
  endTime: string
  visitorName: string
  ticketTypeName: string
  validFrom: string
  validUntil: string
  verifiedAt: string | null
  orderId?: number
  orderNo?: string
  venueId?: number
  venueAddress?: string
  visitorId?: number
}
