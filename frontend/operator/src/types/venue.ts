export type VenueStatus = 'ENABLED' | 'DISABLED'

export interface Venue {
  id: number
  name: string
  address: string
  description: string | null
  coverUrl: string | null
  status: VenueStatus
  longitude: number | null
  latitude: number | null
}

export interface VenueUpdateRequest {
  name: string
  address: string
  description: string
  status: VenueStatus
  longitude: number | null
  latitude: number | null
  coverImage?: File
}
