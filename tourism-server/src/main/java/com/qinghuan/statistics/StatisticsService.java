package com.qinghuan.statistics;

import com.qinghuan.pojo.dto.StatisticsDateRangeDTO;
import com.qinghuan.pojo.vo.StatisticsOverviewVO;
import com.qinghuan.pojo.vo.StatisticsTrendVO;
import com.qinghuan.pojo.vo.TicketTypeStatisticsVO;

import java.util.List;

public interface StatisticsService {

    /** 查询当前运营者所属景点的经营概览。 */
    StatisticsOverviewVO getOverview(StatisticsDateRangeDTO queryDTO);

    /** 查询当前景点按日汇总的销售趋势。 */
    List<StatisticsTrendVO> listTrend(StatisticsDateRangeDTO queryDTO);

    /** 查询当前景点的票种销量排行。 */
    List<TicketTypeStatisticsVO> listTicketTypeStatistics(StatisticsDateRangeDTO queryDTO);
}
