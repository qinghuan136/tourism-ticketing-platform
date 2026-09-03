import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'

const accessTokenKey = 'tourism.access-token'
let authFailureHandler: (() => void) | undefined

export function installAuthFailureHandler(handler: () => void): void {
  authFailureHandler = handler
}

export function getRequestErrorMessage(error: unknown, fallback: string): string {
  if (axios.isAxiosError<{ message?: string; detail?: string }>(error)) {
    return error.response?.data?.message || error.response?.data?.detail || fallback
  }
  return error instanceof Error ? error.message : fallback
}

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10_000,
})

request.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const accessToken = localStorage.getItem(accessTokenKey)
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})

request.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401 && localStorage.getItem(accessTokenKey)) {
      authFailureHandler?.()
    }
    return Promise.reject(error)
  },
)

export { accessTokenKey }
export default request
