import request from '../utils/request'
import type { ApiResponse } from '../types/http'
import type {
  CouponActivity,
  CouponClaimAccepted,
  CouponClaimResult,
  UserCoupon,
  UserCouponQuery,
} from '../types/coupon'

function unwrap<T>(response: ApiResponse<T>): T {
  if (response.code !== 'SUCCESS') throw new Error(response.message)
  return response.data
}

export async function listCouponActivities(venueId: number): Promise<CouponActivity[]> {
  const response = await request.get<ApiResponse<CouponActivity[]>>(`/public/venues/${venueId}/coupon-activities`)
  return unwrap(response.data)
}

export async function claimCoupon(activityId: number): Promise<CouponClaimAccepted> {
  const response = await request.post<ApiResponse<CouponClaimAccepted>>(`/tourist/coupon-activities/${activityId}/claims`)
  return unwrap(response.data)
}

export async function getCouponClaimResult(requestId: string): Promise<CouponClaimResult> {
  const response = await request.get<ApiResponse<CouponClaimResult>>(`/tourist/coupon-claims/${requestId}`)
  return unwrap(response.data)
}

export async function listMyCoupons(query: UserCouponQuery = {}): Promise<UserCoupon[]> {
  const response = await request.get<ApiResponse<UserCoupon[]>>('/tourist/coupons', { params: query })
  return unwrap(response.data)
}
