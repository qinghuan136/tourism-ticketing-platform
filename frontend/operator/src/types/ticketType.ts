export type TicketTypeStatus = 'ENABLED' | 'DISABLED'

export interface TicketType {
  id: number
  venueId: number
  name: string
  description: string | null
  audienceRule: string | null
  basePrice: number
  status: TicketTypeStatus
  createdAt: string
  updatedAt: string
}

export interface TicketTypePageQuery {
  keyword?: string
  page: number
  size: number
}

export interface TicketTypeWriteRequest {
  name: string
  description: string
  audienceRule: string
  basePrice: number
  status: TicketTypeStatus
}
