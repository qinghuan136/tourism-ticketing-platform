import request from '../utils/request'
import type { ApiResponse, PageResult } from '../types/http'
import type {
  AccountStatus,
  StaffAccount,
  StaffCreateRequest,
  StaffPageQuery,
  StaffPasswordResetRequest,
  StaffUpdateRequest,
} from '../types/staff'

const staffPath = '/operator/staff'

function unwrapResponse<T>(response: ApiResponse<T>): T {
  if (response.code !== 'SUCCESS') {
    throw new Error(response.message)
  }
  return response.data
}

/** 分页查询当前运营者所属景点的工作人员。 */
export async function getStaffPage(query: StaffPageQuery): Promise<PageResult<StaffAccount>> {
  const response = await request.get<ApiResponse<PageResult<StaffAccount>>>(`${staffPath}/page`, {
    params: query,
  })
  return unwrapResponse(response.data)
}

/** 启用或停用当前景点的工作人员账号。 */
export async function changeStaffStatus(id: number, status: AccountStatus): Promise<void> {
  const response = await request.post<ApiResponse<null>>(
    `${staffPath}/status/${status}`,
    undefined,
    { params: { id } },
  )
  unwrapResponse(response.data)
}

/** 创建当前景点的工作人员账号。 */
export async function createStaff(payload: StaffCreateRequest): Promise<void> {
  const response = await request.post<ApiResponse<null>>(staffPath, payload)
  unwrapResponse(response.data)
}

/** 修改工作人员姓名和手机号。 */
export async function updateStaff(id: number, payload: StaffUpdateRequest): Promise<void> {
  const response = await request.put<ApiResponse<null>>(`${staffPath}/${id}`, payload)
  unwrapResponse(response.data)
}

/** 重置工作人员登录密码。 */
export async function resetStaffPassword(
  id: number,
  payload: StaffPasswordResetRequest,
): Promise<void> {
  const response = await request.put<ApiResponse<null>>(`${staffPath}/${id}/password`, payload)
  unwrapResponse(response.data)
}
