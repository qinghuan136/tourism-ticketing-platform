import request from '../utils/request'
import type { ApiResponse } from '../types/http'
import type {
  StatisticsDateRange,
  StatisticsOverview,
  StatisticsTrend,
  TicketTypeStatistics,
} from '../types/statistics'

const statisticsPath = '/operator/statistics'

export async function getStatisticsOverview(query: StatisticsDateRange): Promise<StatisticsOverview> {
  const response = await request.get<ApiResponse<StatisticsOverview>>(`${statisticsPath}/overview`, { params: query })
  return response.data.data
}

export async function getStatisticsTrend(query: StatisticsDateRange): Promise<StatisticsTrend[]> {
  const response = await request.get<ApiResponse<StatisticsTrend[]>>(`${statisticsPath}/trend`, { params: query })
  return response.data.data
}

export async function getTicketTypeStatistics(query: StatisticsDateRange): Promise<TicketTypeStatistics[]> {
  const response = await request.get<ApiResponse<TicketTypeStatistics[]>>(`${statisticsPath}/ticket-types`, { params: query })
  return response.data.data
}
