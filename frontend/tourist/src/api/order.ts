import request from '../utils/request'
import type {
  OrderCreatePayload,
  OrderCreated,
  OrderDetail,
  OrderQuery,
  OrderSummary,
} from '../types/order'
import type { ApiResponse, PageResult } from '../types/http'

function unwrap<T>(response: ApiResponse<T>): T {
  if (response.code !== 'SUCCESS') throw new Error(response.message)
  return response.data
}

export async function createOrder(payload: OrderCreatePayload): Promise<OrderCreated> {
  const response = await request.post<ApiResponse<OrderCreated>>('/tourist/orders', payload)
  return unwrap(response.data)
}

export async function pageMyOrders(query: OrderQuery): Promise<PageResult<OrderSummary>> {
  const response = await request.get<ApiResponse<PageResult<OrderSummary>>>('/tourist/orders', { params: query })
  return unwrap(response.data)
}

export async function getMyOrder(orderId: number): Promise<OrderDetail> {
  const response = await request.get<ApiResponse<OrderDetail>>(`/tourist/orders/${orderId}`)
  return unwrap(response.data)
}

export async function payOrder(orderId: number): Promise<void> {
  const response = await request.post<ApiResponse<null>>(`/tourist/orders/${orderId}/pay`)
  unwrap(response.data)
}

export async function cancelOrder(orderId: number): Promise<void> {
  const response = await request.post<ApiResponse<null>>(`/tourist/orders/${orderId}/cancel`)
  unwrap(response.data)
}

export async function refundOrder(orderId: number): Promise<void> {
  const response = await request.post<ApiResponse<null>>(`/tourist/orders/${orderId}/refund`)
  unwrap(response.data)
}
