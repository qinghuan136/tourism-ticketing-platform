export type OrderStatus =
  | 'PENDING_PAYMENT'
  | 'PAID'
  | 'REFUNDING'
  | 'CANCELLED'
  | 'CLOSED'
  | 'COMPLETED'
  | 'REFUNDED'

export interface OrderCreateItem {
  visitorId: number
  sessionTicketTypeId: number
}

export interface OrderCreatePayload {
  sessionId: number
  items: OrderCreateItem[]
  userCouponId?: number
}

export interface OrderCreated {
  id: number
  orderNo: string
  totalAmount: number
  status: OrderStatus
  expireAt: string | null
}

export interface OrderQuery {
  page: number
  size: number
  status?: OrderStatus
}

export interface OrderSummary {
  id: number
  orderNo: string
  venueName: string
  sessionId: number
  visitDate: string
  startTime: string
  endTime: string
  quantity: number
  userCouponId: number | null
  originalAmount: number
  discountAmount: number
  totalAmount: number
  status: OrderStatus
  expireAt: string | null
  createdAt: string
}

export interface TicketSummary {
  id: number
  ticketCode: string
  status: 'VALID' | 'REFUNDING' | 'USED' | 'VOID' | 'EXPIRED'
  validFrom: string
  validUntil: string
  verifiedAt: string | null
}

export interface OrderItem {
  id: number
  visitorId: number
  visitorName: string
  visitorIdType: string
  maskedVisitorIdNumber: string
  sessionTicketTypeId: number
  ticketTypeName: string
  unitPrice: number
  ticket: TicketSummary | null
}

export interface OrderDetail extends OrderSummary {
  purchaserUserId: number | null
  purchaserName: string | null
  purchaserPhone: string | null
  paymentNo: string | null
  refundNo: string | null
  paidAt: string | null
  cancelledAt: string | null
  closedAt: string | null
  completedAt: string | null
  refundRequestedAt: string | null
  refundAt: string | null
  items: OrderItem[]
}
