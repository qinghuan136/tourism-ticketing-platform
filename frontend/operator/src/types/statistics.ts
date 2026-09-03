export interface StatisticsDateRange {
  startDate: string
  endDate: string
}

export interface StatisticsOverview {
  paidOrderCount: number
  grossRevenue: number
  refundAmount: number
  netRevenue: number
  soldTicketCount: number
  verifiedTicketCount: number
}

export interface StatisticsTrend {
  statisticDate: string
  paidOrderCount: number
  grossRevenue: number
  soldTicketCount: number
}

export interface TicketTypeStatistics {
  ticketTypeName: string
  soldQuantity: number
  salesAmount: number
}
