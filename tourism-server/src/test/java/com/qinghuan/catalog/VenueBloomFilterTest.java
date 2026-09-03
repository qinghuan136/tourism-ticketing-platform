package com.qinghuan.catalog;

import com.qinghuan.common.constant.cacheKeys.VenueConstant;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VenueBloomFilterTest {

    @Test
    @SuppressWarnings("unchecked")
    void initializesFilterAndLoadsAllVenueIds() {
        RedissonClient redissonClient = mock(RedissonClient.class);
        RBloomFilter<Long> filter = mock(RBloomFilter.class);
        when(redissonClient.<Long>getBloomFilter(
                VenueConstant.VENUE_ID_BLOOM_FILTER)).thenReturn(filter);

        VenueBloomFilter venueBloomFilter = new VenueBloomFilter(redissonClient);
        venueBloomFilter.initialize(List.of(1L, 2L));

        verify(filter).tryInit(
                VenueConstant.VENUE_ID_EXPECTED_INSERTIONS,
                VenueConstant.VENUE_ID_FALSE_PROBABILITY);
        verify(filter).add(1L);
        verify(filter).add(2L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void delegatesMembershipCheckToRedisBloomFilter() {
        RedissonClient redissonClient = mock(RedissonClient.class);
        RBloomFilter<Long> filter = mock(RBloomFilter.class);
        when(redissonClient.<Long>getBloomFilter(
                VenueConstant.VENUE_ID_BLOOM_FILTER)).thenReturn(filter);
        when(filter.contains(1L)).thenReturn(true);
        when(filter.contains(999L)).thenReturn(false);

        VenueBloomFilter venueBloomFilter = new VenueBloomFilter(redissonClient);

        assertTrue(venueBloomFilter.mightContain(1L));
        assertFalse(venueBloomFilter.mightContain(999L));
    }
}
