package com.qinghuan.statistics;

import com.qinghuan.annotation.RequireRole;
import com.qinghuan.common.response.ApiResponse;
import com.qinghuan.pojo.dto.StatisticsDateRangeDTO;
import com.qinghuan.pojo.enums.AccountRole;
import com.qinghuan.pojo.vo.StatisticsOverviewVO;
import com.qinghuan.pojo.vo.StatisticsTrendVO;
import com.qinghuan.pojo.vo.TicketTypeStatisticsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 运营端基础经营统计接口。 */
@RestController
@RequestMapping("/operator/statistics")
@Tag(name = "运营统计", description = "当前运营者所属景点的经营概览、趋势与票种排行")
@SecurityRequirement(name = "BearerAuth")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    /** 查询支付、退款、售票和核销等核心指标。 */
    @GetMapping("/overview")
    @RequireRole(AccountRole.OPERATOR)
    @Operation(summary = "查询经营概览")
    public ApiResponse<StatisticsOverviewVO> getOverview(
            @ParameterObject @Valid StatisticsDateRangeDTO queryDTO) {
        return ApiResponse.success(statisticsService.getOverview(queryDTO));
    }

    /** 按支付日期汇总每天的订单和售票情况。 */
    @GetMapping("/trend")
    @RequireRole(AccountRole.OPERATOR)
    @Operation(summary = "查询每日销售趋势")
    public ApiResponse<List<StatisticsTrendVO>> listTrend(
            @ParameterObject @Valid StatisticsDateRangeDTO queryDTO) {
        return ApiResponse.success(statisticsService.listTrend(queryDTO));
    }

    /** 按下单时的票种快照统计销量和销售额。 */
    @GetMapping("/ticket-types")
    @RequireRole(AccountRole.OPERATOR)
    @Operation(summary = "查询票种销售排行")
    public ApiResponse<List<TicketTypeStatisticsVO>> listTicketTypeStatistics(
            @ParameterObject @Valid StatisticsDateRangeDTO queryDTO) {
        return ApiResponse.success(statisticsService.listTicketTypeStatistics(queryDTO));
    }
}
