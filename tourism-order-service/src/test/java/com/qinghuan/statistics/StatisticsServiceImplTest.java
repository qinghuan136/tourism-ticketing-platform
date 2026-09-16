package com.qinghuan.statistics;

import cn.hutool.json.JSONUtil;
import com.qinghuan.auth.context.UserContext;
import com.qinghuan.auth.model.LoginUser;
import com.qinghuan.common.exception.BusinessException;
import com.qinghuan.common.exception.ErrorCode;
import com.qinghuan.pojo.dto.StatisticsDateRangeDTO;
import com.qinghuan.pojo.enums.AccountRole;
import com.qinghuan.pojo.vo.StatisticsOverviewVO;
import com.qinghuan.pojo.vo.StatisticsTrendVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceImplTest {

    @Mock
    private StatisticsMapper statisticsMapper;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private StatisticsServiceImpl statisticsService;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        statisticsService = new StatisticsServiceImpl(
                statisticsMapper, stringRedisTemplate);
        UserContext.set(new LoginUser(7L, "operator", AccountRole.OPERATOR, 10L));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void getOverview_shouldUseCurrentVenueAndCalculateNetRevenue() {
        StatisticsDateRangeDTO query = query("2026-08-01", "2026-08-03");
        StatisticsOverviewVO overview = new StatisticsOverviewVO();
        overview.setGrossRevenue(new BigDecimal("150.00"));
        overview.setRefundAmount(new BigDecimal("50.00"));
        when(statisticsMapper.getOverview(
                10L,
                LocalDateTime.parse("2026-08-01T00:00:00"),
                LocalDateTime.parse("2026-08-04T00:00:00")))
                .thenReturn(overview);

        StatisticsOverviewVO result = statisticsService.getOverview(query);

        assertEquals(new BigDecimal("100.00"), result.getNetRevenue());
        verify(statisticsMapper).getOverview(
                10L,
                LocalDateTime.parse("2026-08-01T00:00:00"),
                LocalDateTime.parse("2026-08-04T00:00:00"));
    }

    @Test
    void getOverview_shouldReturnRedisResultWithoutQueryingDatabase() {
        StatisticsOverviewVO cached = new StatisticsOverviewVO();
        cached.setNetRevenue(new BigDecimal("88.00"));
        when(valueOperations.get(
                "statistics:overview:10:2026-08-01:2026-08-03"))
                .thenReturn(JSONUtil.toJsonStr(cached));

        StatisticsOverviewVO result = statisticsService.getOverview(
                query("2026-08-01", "2026-08-03"));

        assertEquals(0, new BigDecimal("88.00").compareTo(result.getNetRevenue()));
        verify(statisticsMapper, never()).getOverview(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void listTrend_shouldDeserializeCachedList() {
        StatisticsTrendVO trend = new StatisticsTrendVO();
        trend.setStatisticDate(LocalDate.parse("2026-08-01"));
        trend.setPaidOrderCount(3L);
        when(valueOperations.get(
                "statistics:trend:10:2026-08-01:2026-08-03"))
                .thenReturn(JSONUtil.toJsonStr(List.of(trend)));

        List<StatisticsTrendVO> result = statisticsService.listTrend(
                query("2026-08-01", "2026-08-03"));

        assertEquals(LocalDate.parse("2026-08-01"), result.get(0).getStatisticDate());
        assertEquals(3L, result.get(0).getPaidOrderCount());
        verify(statisticsMapper, never()).listTrend(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getOverview_shouldRejectReversedDateRange() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> statisticsService.getOverview(query("2026-08-03", "2026-08-01")));

        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }

    private StatisticsDateRangeDTO query(String startDate, String endDate) {
        StatisticsDateRangeDTO query = new StatisticsDateRangeDTO();
        query.setStartDate(LocalDate.parse(startDate));
        query.setEndDate(LocalDate.parse(endDate));
        return query;
    }
}
