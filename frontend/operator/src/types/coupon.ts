export type CouponActivityStatus = 'DRAFT' | 'PUBLISHED' | 'ENDED' | 'CANCELLED'

export interface CouponActivityPageQuery {
  keyword?: string
  status?: CouponActivityStatus
  page: number
  size: number
}

export interface CouponActivityWriteRequest {
  name: string
  thresholdAmount: number
  discountAmount: number
  totalStock: number
  claimStartAt: string
  claimEndAt: string
  validFrom: string
  validUntil: string
}

export interface CouponActivity extends CouponActivityWriteRequest {
  id: number
  venueId: number
  remainingStock: number
  status: CouponActivityStatus
  cacheReady: boolean
  preheatedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface CouponActivityCreated {
  id: number
  status: CouponActivityStatus
}
