import request from '../utils/request'
import type { ApiResponse, PageResult } from '../types/http'
import type {
  CouponActivity,
  CouponActivityCreated,
  CouponActivityPageQuery,
  CouponActivityWriteRequest,
} from '../types/coupon'

const couponPath = '/operator/coupon-activities'

export async function getCouponActivityPage(query: CouponActivityPageQuery): Promise<PageResult<CouponActivity>> {
  const response = await request.get<ApiResponse<PageResult<CouponActivity>>>(couponPath, { params: query })
  return response.data.data
}

export async function getCouponActivity(id: number): Promise<CouponActivity> {
  const response = await request.get<ApiResponse<CouponActivity>>(`${couponPath}/${id}`)
  return response.data.data
}

export async function createCouponActivity(payload: CouponActivityWriteRequest): Promise<CouponActivityCreated> {
  const response = await request.post<ApiResponse<CouponActivityCreated>>(couponPath, payload)
  return response.data.data
}

export async function updateCouponActivity(id: number, payload: CouponActivityWriteRequest): Promise<void> {
  await request.put<ApiResponse<null>>(`${couponPath}/${id}`, payload)
}

export async function publishCouponActivity(id: number): Promise<void> {
  await request.post<ApiResponse<null>>(`${couponPath}/${id}/publish`)
}

export async function cancelCouponActivity(id: number): Promise<void> {
  await request.post<ApiResponse<null>>(`${couponPath}/${id}/cancel`)
}
