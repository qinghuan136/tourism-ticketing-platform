package com.qinghuan.venue;

import com.qinghuan.common.constant.cacheKeys.VenueConstant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VenueGeoServiceImplTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private VenueMapper venueMapper;
    @Mock
    private GeoOperations<String, String> geoOperations;

    private VenueGeoServiceImpl venueGeoService;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForGeo()).thenReturn(geoOperations);
        venueGeoService = new VenueGeoServiceImpl(
                stringRedisTemplate, venueMapper);
    }

    @Test
    void searchNearby_shouldReturnRedisDistanceOrder() {
        RedisGeoCommands.GeoLocation<String> first =
                new RedisGeoCommands.GeoLocation<>(
                        "20", new Point(113.32, 23.10));
        RedisGeoCommands.GeoLocation<String> second =
                new RedisGeoCommands.GeoLocation<>(
                        "10", new Point(113.33, 23.11));
        GeoResults<RedisGeoCommands.GeoLocation<String>> redisResults =
                new GeoResults<>(List.of(
                        new GeoResult<>(first,
                                new Distance(1.25, Metrics.KILOMETERS)),
                        new GeoResult<>(second,
                                new Distance(2.50, Metrics.KILOMETERS))));

        when(geoOperations.search(
                eq(VenueConstant.VENUE_GEO_KEY),
                any(GeoReference.class),
                any(Distance.class),
                any(RedisGeoCommands.GeoSearchCommandArgs.class)))
                .thenReturn(redisResults);

        List<VenueGeoResult> result = venueGeoService.searchNearby(
                113.32, 23.10, 5.0, 20);

        assertEquals(List.of(20L, 10L), result.stream()
                .map(VenueGeoResult::venueId)
                .toList());
        assertEquals(1.25, result.get(0).distanceKm());
    }
}
