export interface CatalogVenue {
  id: number
  name: string
  address: string
  description: string
  coverUrl: string | null
  minimumPrice: number | null
}

export interface NearbyVenue {
  id: number
  name: string
  address: string
  description: string
  coverUrl: string | null
  distanceKm: number
}

export type SessionSaleState = 'NOT_STARTED' | 'ON_SALE' | 'SOLD_OUT'

export interface SellableTicketType {
  sessionTicketTypeId: number
  ticketTypeName: string
  description: string | null
  audienceRule: string | null
  salePrice: number
  remainingQuantity: number | null
}

export interface SellableSession {
  id: number
  visitDate: string
  startTime: string
  endTime: string
  bookingStartAt: string
  bookingEndAt: string
  saleState: SessionSaleState
  remainingCapacity: number | null
  ticketTypes: SellableTicketType[]
}

export interface VenueQuery {
  keyword?: string
  page: number
  size: number
}

export interface NearbyByCoordinateQuery {
  longitude: number
  latitude: number
  radiusKm?: number
  limit?: number
}

export interface NearbyByNameQuery {
  name: string
  city?: string
  radiusKm?: number
  limit?: number
}
