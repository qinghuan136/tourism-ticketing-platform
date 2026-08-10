package com.qinghuan.statistics;

import com.qinghuan.pojo.vo.StatisticsOverviewVO;
import com.qinghuan.pojo.vo.StatisticsTrendVO;
import com.qinghuan.pojo.vo.TicketTypeStatisticsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface StatisticsMapper {

    /** 汇总指定景点在日期范围内的核心指标。 */
    StatisticsOverviewVO getOverview(@Param("venueId") Long venueId,
                                     @Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime);

    /** 按支付日期汇总订单数、实收金额和售票数。 */
    List<StatisticsTrendVO> listTrend(@Param("venueId") Long venueId,
                                      @Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime);

    /** 按订单明细中的票种快照汇总销量与销售额。 */
    List<TicketTypeStatisticsVO> listTicketTypeStatistics(
            @Param("venueId") Long venueId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}
