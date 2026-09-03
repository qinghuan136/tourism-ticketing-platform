import request from '../utils/request'
import type {
  CatalogVenue,
  NearbyByCoordinateQuery,
  NearbyByNameQuery,
  NearbyVenue,
  SellableSession,
  VenueQuery,
} from '../types/catalog'
import type { ApiResponse, PageResult } from '../types/http'

function unwrap<T>(response: ApiResponse<T>): T {
  if (response.code !== 'SUCCESS') throw new Error(response.message)
  return response.data
}

export async function pageVenues(query: VenueQuery): Promise<PageResult<CatalogVenue>> {
  const response = await request.get<ApiResponse<PageResult<CatalogVenue>>>('/public/venues', { params: query })
  return unwrap(response.data)
}

export async function getVenue(venueId: number): Promise<CatalogVenue> {
  const response = await request.get<ApiResponse<CatalogVenue>>(`/public/venues/${venueId}`)
  return unwrap(response.data)
}

export async function listVenueSessions(venueId: number, visitDate: string): Promise<SellableSession[]> {
  const response = await request.get<ApiResponse<SellableSession[]>>(`/public/venues/${venueId}/sessions`, {
    params: { visitDate },
  })
  return unwrap(response.data)
}

export async function listNearbyByCoordinate(query: NearbyByCoordinateQuery): Promise<NearbyVenue[]> {
  const response = await request.get<ApiResponse<NearbyVenue[]>>('/public/venues/nearby', { params: query })
  return unwrap(response.data)
}

export async function listNearbyByName(query: NearbyByNameQuery): Promise<NearbyVenue[]> {
  const response = await request.get<ApiResponse<NearbyVenue[]>>('/public/venues/nearby/by-name', { params: query })
  return unwrap(response.data)
}
