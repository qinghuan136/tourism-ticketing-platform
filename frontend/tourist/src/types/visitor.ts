export type VisitorStatus = 'ACTIVE' | 'DISABLED'

export interface Visitor {
  id: number
  name: string
  idType: string
  maskedIdNumber: string
  phone: string | null
  status: VisitorStatus
  createdAt: string
  updatedAt: string
}

export interface VisitorCreatePayload {
  name: string
  idType: string
  idNumber: string
  phone: string | null
}

export interface VisitorUpdatePayload {
  name: string
  phone: string | null
}
