import request from '../utils/request'
import type { ApiResponse } from '../types/http'
import type { Venue, VenueUpdateRequest } from '../types/venue'

const venuePath = '/operator/venue'

export async function getCurrentVenue(): Promise<Venue> {
  const response = await request.get<ApiResponse<Venue>>(venuePath)
  return response.data.data
}

export async function updateCurrentVenue(payload: VenueUpdateRequest): Promise<void> {
  const formData = new FormData()
  formData.append('name', payload.name)
  formData.append('address', payload.address)
  formData.append('description', payload.description)
  formData.append('status', payload.status)
  if (payload.longitude !== null) formData.append('longitude', String(payload.longitude))
  if (payload.latitude !== null) formData.append('latitude', String(payload.latitude))
  if (payload.coverImage) formData.append('coverImage', payload.coverImage)

  await request.put<ApiResponse<null>>(venuePath, formData)
}
