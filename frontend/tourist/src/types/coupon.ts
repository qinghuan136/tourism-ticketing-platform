export type CouponClaimDisplayState = 'NOT_STARTED' | 'IN_PROGRESS' | 'SOLD_OUT'
export type CouponClaimStatus = 'PENDING' | 'SUCCESS' | 'FAILED'
export type UserCouponStatus = 'AVAILABLE' | 'LOCKED' | 'USED' | 'EXPIRED'

export interface CouponActivity {
  id: number
  venueId: number
  name: string
  thresholdAmount: number
  discountAmount: number
  claimStartAt: string
  claimEndAt: string
  validFrom: string
  validUntil: string
  claimState: CouponClaimDisplayState
}

export interface CouponClaimAccepted {
  requestId: string
  status: CouponClaimStatus
}

export interface CouponClaimResult {
  requestId: string
  activityId: number
  status: CouponClaimStatus
  userCouponId: number | null
  failureReason: string | null
  processedAt: string | null
}

export interface UserCoupon {
  id: number
  activityId: number
  venueId: number
  venueName: string
  couponName: string
  thresholdAmount: number
  discountAmount: number
  validFrom: string
  validUntil: string
  status: UserCouponStatus
  acquiredAt: string
}

export interface UserCouponQuery {
  venueId?: number
  status?: UserCouponStatus
  orderAmount?: number
}
