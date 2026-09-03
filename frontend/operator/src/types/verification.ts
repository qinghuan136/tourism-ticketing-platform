import type { TicketStatus } from './order'

export type VerificationResult = 'SUCCESS' | 'FAILED'

export interface VerificationRequest {
  requestNo: string
  ticketCode: string
  deviceNo?: string
}

export interface VerificationTicket {
  id: number
  ticketCode: string
  status: TicketStatus
  venueName: string
  visitorName: string
  ticketTypeName: string
  visitDate: string
  startTime: string
  endTime: string
}

export interface VerificationResponse {
  requestNo: string
  result: VerificationResult
  failureReason: string | null
  verifiedAt: string
  ticket: VerificationTicket
}

export interface VerificationRecord {
  id: number
  requestNo: string
  verifiedAt: string
  ticketCode: string
  visitorName: string
  ticketTypeName: string
  verifierId: number
  verifierName: string
  result: VerificationResult
  failureReason: string | null
  deviceNo: string | null
}

export interface VerificationPageQuery {
  ticketCode?: string
  result?: VerificationResult
  verificationDate?: string
  page: number
  size: number
}
