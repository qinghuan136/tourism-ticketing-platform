import request from '../utils/request'
import type { ApiResponse, PageResult } from '../types/http'
import type { OrderDetail, OrderPageQuery, OrderSummary } from '../types/order'

const orderPath = '/operator/orders'

export async function getOrderPage(query: OrderPageQuery): Promise<PageResult<OrderSummary>> {
  const response = await request.get<ApiResponse<PageResult<OrderSummary>>>(orderPath, { params: query })
  return response.data.data
}

export async function getOrderDetail(id: number): Promise<OrderDetail> {
  const response = await request.get<ApiResponse<OrderDetail>>(`${orderPath}/${id}`)
  return response.data.data
}
