import request from '../utils/request'
import type { ApiResponse, PageResult } from '../types/http'
import type { CreateVenueCommentRequest, VenueComment } from '../types/comment'

function unwrap<T>(response: ApiResponse<T>): T {
  if (response.code !== 'SUCCESS') throw new Error(response.message)
  return response.data
}

export async function pageVenueComments(
  venueId: number,
  page: number,
  size: number,
): Promise<PageResult<VenueComment>> {
  const response = await request.get<ApiResponse<PageResult<VenueComment>>>(`/public/venues/${venueId}/comments`, {
    params: { page, size },
  })
  return unwrap(response.data)
}

export async function createVenueComment(
  venueId: number,
  payload: CreateVenueCommentRequest,
): Promise<number> {
  const response = await request.post<ApiResponse<number>>(`/tourist/venues/${venueId}/comments`, payload)
  return unwrap(response.data)
}

export async function likeVenueComment(commentId: number): Promise<void> {
  const response = await request.put<ApiResponse<void>>(`/tourist/comments/${commentId}/like`)
  return unwrap(response.data)
}

export async function unlikeVenueComment(commentId: number): Promise<void> {
  const response = await request.delete<ApiResponse<void>>(`/tourist/comments/${commentId}/like`)
  return unwrap(response.data)
}
