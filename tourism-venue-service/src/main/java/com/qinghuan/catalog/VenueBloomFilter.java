package com.qinghuan.catalog;

import com.qinghuan.common.constant.cacheKeys.VenueConstant;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 景点 ID 布隆过滤器。
 *
 * Redis 中的过滤器由所有服务实例共享。
 */
@Component
public class VenueBloomFilter {

    private final RBloomFilter<Long> bloomFilter;

    public VenueBloomFilter(RedissonClient redissonClient) {
        this.bloomFilter = redissonClient.getBloomFilter(
                VenueConstant.VENUE_ID_BLOOM_FILTER);
    }

    /** 初始化过滤器参数，并幂等写入当前数据库中的景点 ID。 */
    public void initialize(List<Long> venueIds) {
        bloomFilter.tryInit(
                VenueConstant.VENUE_ID_EXPECTED_INSERTIONS,
                VenueConstant.VENUE_ID_FALSE_PROBABILITY);
        venueIds.forEach(bloomFilter::add);
    }

    /** false 表示景点一定不存在，true 表示景点可能存在。 */
    public boolean mightContain(Long venueId) {
        return bloomFilter.contains(venueId);
    }
}
