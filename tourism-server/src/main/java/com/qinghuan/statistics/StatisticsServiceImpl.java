package com.qinghuan.statistics;

import cn.hutool.json.JSONUtil;
import com.qinghuan.auth.context.UserContext;
import com.qinghuan.common.exception.BusinessException;
import com.qinghuan.common.exception.ErrorCode;
import com.qinghuan.pojo.dto.StatisticsDateRangeDTO;
import com.qinghuan.pojo.vo.StatisticsOverviewVO;
import com.qinghuan.pojo.vo.StatisticsTrendVO;
import com.qinghuan.pojo.vo.TicketTypeStatisticsVO;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    private static final String CACHE_KEY_PREFIX = "statistics:";
    private static final long CACHE_TTL_SECONDS = 60L;

    private final StatisticsMapper statisticsMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public StatisticsServiceImpl(StatisticsMapper statisticsMapper,
                                 StringRedisTemplate stringRedisTemplate) {
        this.statisticsMapper = statisticsMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public StatisticsOverviewVO getOverview(StatisticsDateRangeDTO queryDTO) {
        DateRange range = toDateRange(queryDTO);
        Long venueId = currentVenueId();
        String cacheKey = buildCacheKey("overview", venueId, queryDTO);
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StringUtils.hasText(cached)) {
            return JSONUtil.toBean(cached, StatisticsOverviewVO.class);
        }

        StatisticsOverviewVO overview = statisticsMapper.getOverview(
                venueId, range.startTime(), range.endTime());

        // 净收入按统计期内的实收金额减去同一期间发生的退款计算。
        overview.setNetRevenue(overview.getGrossRevenue().subtract(overview.getRefundAmount()));
        cacheResult(cacheKey, overview);
        return overview;
    }

    @Override
    public List<StatisticsTrendVO> listTrend(StatisticsDateRangeDTO queryDTO) {
        DateRange range = toDateRange(queryDTO);
        Long venueId = currentVenueId();
        String cacheKey = buildCacheKey("trend", venueId, queryDTO);
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StringUtils.hasText(cached)) {
            return JSONUtil.toList(cached, StatisticsTrendVO.class);
        }

        List<StatisticsTrendVO> result = statisticsMapper.listTrend(
                venueId, range.startTime(), range.endTime());
        cacheResult(cacheKey, result);
        return result;
    }

    @Override
    public List<TicketTypeStatisticsVO> listTicketTypeStatistics(
            StatisticsDateRangeDTO queryDTO) {
        DateRange range = toDateRange(queryDTO);
        Long venueId = currentVenueId();
        String cacheKey = buildCacheKey("ticket-types", venueId, queryDTO);
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StringUtils.hasText(cached)) {
            return JSONUtil.toList(cached, TicketTypeStatisticsVO.class);
        }

        List<TicketTypeStatisticsVO> result =
                statisticsMapper.listTicketTypeStatistics(
                        venueId, range.startTime(), range.endTime());
        cacheResult(cacheKey, result);
        return result;
    }

    /** 相同景点、日期范围和统计类型共享同一份短期查询结果。 */
    private String buildCacheKey(String type,
                                 Long venueId,
                                 StatisticsDateRangeDTO queryDTO) {
        return CACHE_KEY_PREFIX + type + ":" + venueId + ":"
                + queryDTO.getStartDate() + ":" + queryDTO.getEndDate();
    }

    private void cacheResult(String cacheKey, Object result) {
        stringRedisTemplate.opsForValue().set(
                cacheKey,
                JSONUtil.toJsonStr(result),
                CACHE_TTL_SECONDS,
                TimeUnit.SECONDS);
    }

    private Long currentVenueId() {
        // 数据范围只取 JWT 中的 venueId，避免客户端指定其他景点。
        return UserContext.getRequired().venueId();
    }

    private DateRange toDateRange(StatisticsDateRangeDTO queryDTO) {
        if (queryDTO.getEndDate().isBefore(queryDTO.getStartDate())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "结束日期不能早于开始日期");
        }

        // 使用左闭右开区间，完整包含 endDate 当天且无需拼接 23:59:59。
        return new DateRange(
                queryDTO.getStartDate().atStartOfDay(),
                queryDTO.getEndDate().plusDays(1).atStartOfDay());
    }

    private record DateRange(LocalDateTime startTime, LocalDateTime endTime) {
    }
}
