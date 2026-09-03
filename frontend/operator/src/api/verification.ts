import request from '../utils/request'
import type { ApiResponse, PageResult } from '../types/http'
import type {
  VerificationPageQuery,
  VerificationRecord,
  VerificationRequest,
  VerificationResponse,
} from '../types/verification'

const verificationPath = '/operator/verifications'

export async function verifyTicket(payload: VerificationRequest): Promise<VerificationResponse> {
  const response = await request.post<ApiResponse<VerificationResponse>>(verificationPath, payload)
  return response.data.data
}

export async function getVerificationRecordPage(query: VerificationPageQuery): Promise<PageResult<VerificationRecord>> {
  const response = await request.get<ApiResponse<PageResult<VerificationRecord>>>(verificationPath, { params: query })
  return response.data.data
}
