/** 与后端 ApiResponse 和 PageResult 的响应结构保持一致。 */
export interface ApiResponse<T> {
  code: string
  message: string
  data: T
}

export interface PageResult<T> {
  items: T[]
  total: number
  page: number
  size: number
}
