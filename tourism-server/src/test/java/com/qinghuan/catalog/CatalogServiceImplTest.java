package com.qinghuan.catalog;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qinghuan.common.exception.BusinessException;
import com.qinghuan.common.exception.ErrorCode;
import com.qinghuan.config.oss.OssUtils;
import com.qinghuan.pojo.dto.CatalogVenuePageQueryDTO;
import com.qinghuan.pojo.dto.NearbyVenueByNameQueryDTO;
import com.qinghuan.pojo.dto.NearbyVenueQueryDTO;
import com.qinghuan.pojo.vo.CatalogVenueVO;
import com.qinghuan.pojo.vo.NearbyVenueVO;
import com.qinghuan.pojo.vo.PageResult;
import com.qinghuan.pojo.vo.SellableSessionVO;
import com.qinghuan.redis.CacheClient;
import com.qinghuan.session.SessionInventoryService;
import com.qinghuan.session.SessionService;
import com.qinghuan.venue.VenueGeoResult;
import com.qinghuan.venue.VenueGeoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("游客端可售内容查询")
class CatalogServiceImplTest {

    @Mock
    private CatalogMapper catalogMapper;
    @Mock
    private OssUtils ossUtils;
    @Mock
    private CacheClient cacheClient;
    @Mock
    private VenueBloomFilter venueBloomFilter;
    @Mock
    private SessionService sessionService;
    @Mock
    private SessionInventoryService sessionInventoryService;
    @Mock
    private VenueGeoService venueGeoService;
    @Mock
    private GeocodingService geocodingService;

    private CatalogServiceImpl catalogService;

    @BeforeEach
    void setUp() {
        catalogService = new CatalogServiceImpl(
                catalogMapper,
                ossUtils,
                cacheClient,
                Caffeine.<Long, CatalogVenueVO>newBuilder().build(),
                venueBloomFilter,
                sessionService,
                sessionInventoryService,
                venueGeoService,
                geocodingService);
    }

    @AfterEach
    void clearPage() {
        PageHelper.clearPage();
    }

    @Test
    void pageVenues_shouldReturnPageAndResolveCoverUrl() {
        CatalogVenuePageQueryDTO query = new CatalogVenuePageQueryDTO();
        query.setKeyword(" 科技馆 ");
        CatalogVenueVO venue = venue(10L, "venue/cover.jpg");
        Page<CatalogVenueVO> page = new Page<>(1, 20);
        page.add(venue);
        page.setTotal(1);
        when(catalogMapper.listEnabledVenues(query)).thenReturn(page);
        when(ossUtils.getPublicUrl("venue/cover.jpg"))
                .thenReturn("https://tourism-test.oss-cn-hangzhou.aliyuncs.com/venue/cover.jpg");

        PageResult<CatalogVenueVO> result = catalogService.pageVenues(query);

        assertEquals("科技馆", query.getKeyword());
        assertEquals(1, result.total());
        assertEquals(
                "https://tourism-test.oss-cn-hangzhou.aliyuncs.com/venue/cover.jpg",
                result.items().get(0).getCoverUrl());
    }

    @Test
    void getVenue_shouldReturnEnabledVenue() {
        CatalogVenueVO venue = venue(10L, "venue/cover.jpg");
        when(cacheClient.queryWithPassThrough(
                anyString(), eq(10L), eq(CatalogVenueVO.class),
                any(), anyLong(), eq(java.util.concurrent.TimeUnit.SECONDS)))
                .thenReturn(venue);
        when(ossUtils.getPublicUrl("venue/cover.jpg")).thenReturn("https://example.com/cover.jpg");

        CatalogVenueVO result = catalogService.getVenue(10L);

        assertEquals(10L, result.getId());
        assertEquals("https://example.com/cover.jpg", result.getCoverUrl());
    }

    @Test
    void getVenue_shouldReturnNotFoundWhenVenueIsUnavailable() {
        BusinessException exception = assertThrows(
                BusinessException.class, () -> catalogService.getVenue(10L));

        assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getVenue_shouldSkipDatabaseWhenBloomFilterRejectsId() {
        when(venueBloomFilter.mightContain(999L)).thenReturn(false);
        when(cacheClient.queryWithPassThrough(
                anyString(), eq(999L), eq(CatalogVenueVO.class),
                any(), anyLong(), eq(java.util.concurrent.TimeUnit.SECONDS)))
                .thenAnswer(invocation -> {
                    Function<Long, CatalogVenueVO> fallback = invocation.getArgument(3);
                    return fallback.apply(999L);
                });

        assertThrows(BusinessException.class,
                () -> catalogService.getVenue(999L));

        verify(catalogMapper, never()).findEnabledVenue(999L);
    }

    @Test
    void listSellableSessions_shouldReturnEmptyListForEnabledVenue() {
        LocalDate visitDate = LocalDate.now().plusDays(1);
        when(cacheClient.queryWithPassThrough(
                anyString(), eq(10L), eq(CatalogVenueVO.class),
                any(), anyLong(), eq(java.util.concurrent.TimeUnit.SECONDS)))
                .thenReturn(venue(10L, null));
        when(catalogMapper.listVisibleSessionIds(
                eq(10L), eq(visitDate), any(LocalDateTime.class)))
                .thenReturn(List.of());

        List<SellableSessionVO> result =
                catalogService.listSellableSessions(10L, visitDate);

        assertEquals(List.of(), result);
        verify(catalogMapper).listVisibleSessionIds(
                eq(10L), eq(visitDate), any(LocalDateTime.class));
    }

    @Test
    void listNearbyVenues_shouldKeepRedisOrderAndReuseVenueCache() {
        NearbyVenueQueryDTO query = new NearbyVenueQueryDTO();
        query.setLongitude(113.32);
        query.setLatitude(23.10);

        when(venueGeoService.searchNearby(113.32, 23.10, 5.0, 20))
                .thenReturn(List.of(
                        new VenueGeoResult(20L, 1.25),
                        new VenueGeoResult(10L, 2.5)));
        when(cacheClient.queryWithPassThrough(
                anyString(), eq(20L), eq(CatalogVenueVO.class),
                any(), anyLong(), eq(java.util.concurrent.TimeUnit.SECONDS)))
                .thenReturn(venue(20L, null));
        when(cacheClient.queryWithPassThrough(
                anyString(), eq(10L), eq(CatalogVenueVO.class),
                any(), anyLong(), eq(java.util.concurrent.TimeUnit.SECONDS)))
                .thenReturn(venue(10L, null));

        List<NearbyVenueVO> result = catalogService.listNearbyVenues(query);

        assertEquals(List.of(20L, 10L), result.stream()
                .map(NearbyVenueVO::getId)
                .toList());
        assertEquals("1.25", result.get(0).getDistanceKm().toPlainString());
        verify(cacheClient).queryWithPassThrough(
                anyString(), eq(20L), eq(CatalogVenueVO.class),
                any(), anyLong(), eq(java.util.concurrent.TimeUnit.SECONDS));
        verify(cacheClient).queryWithPassThrough(
                anyString(), eq(10L), eq(CatalogVenueVO.class),
                any(), anyLong(), eq(java.util.concurrent.TimeUnit.SECONDS));
    }

    @Test
    void listNearbyVenuesByName_shouldResolveAndReuseCoordinateSearch() {
        NearbyVenueByNameQueryDTO query = new NearbyVenueByNameQueryDTO();
        query.setName(" 广州塔 ");
        query.setCity(" 广州 ");
        when(geocodingService.geocode("广州塔", "广州"))
                .thenReturn(new GeoCoordinate(113.32, 23.10));
        when(venueGeoService.searchNearby(113.32, 23.10, 5.0, 20))
                .thenReturn(List.of());

        List<NearbyVenueVO> result =
                catalogService.listNearbyVenuesByName(query);

        assertEquals(List.of(), result);
        verify(venueGeoService).searchNearby(113.32, 23.10, 5.0, 20);
    }

    private CatalogVenueVO venue(Long id, String coverObjectKey) {
        CatalogVenueVO venue = new CatalogVenueVO();
        venue.setId(id);
        venue.setName("海湾科技馆");
        venue.setCoverUrl(coverObjectKey);
        return venue;
    }
}
