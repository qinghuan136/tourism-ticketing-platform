package com.qinghuan.catalog;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qinghuan.common.constant.cacheKeys.VenueConstant;
import com.qinghuan.common.exception.BusinessException;
import com.qinghuan.common.exception.ErrorCode;
import com.qinghuan.config.oss.OssUtils;
import com.qinghuan.pojo.dto.CatalogVenuePageQueryDTO;
import com.qinghuan.pojo.dto.NearbyVenueByNameQueryDTO;
import com.qinghuan.pojo.dto.NearbyVenueQueryDTO;
import com.qinghuan.pojo.enums.CatalogSessionSaleState;
import com.qinghuan.pojo.vo.*;
import com.qinghuan.redis.CacheClient;
import com.qinghuan.session.SessionInventoryService;
import com.qinghuan.session.SessionService;
import com.qinghuan.venue.VenueGeoResult;
import com.qinghuan.venue.VenueGeoService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CatalogServiceImpl implements CatalogService {

    private final CatalogMapper catalogMapper;
    private final OssUtils ossUtils;
    private final CacheClient cacheClient;
    private final Cache<Long, CatalogVenueVO> venueDetailLocalCache;
    private final SessionService sessionService;
    private final SessionInventoryService sessionInventoryService;
    private final VenueGeoService venueGeoService;
    private final GeocodingService geocodingService;

    public CatalogServiceImpl(
            CatalogMapper catalogMapper,
            OssUtils ossUtils,
            CacheClient cacheClient,
            @Qualifier("venueDetailLocalCache")
            Cache<Long, CatalogVenueVO> venueDetailLocalCache,
            SessionService sessionService,
            SessionInventoryService sessionInventoryService,
            VenueGeoService venueGeoService,
            GeocodingService geocodingService) {
        this.catalogMapper = catalogMapper;
        this.ossUtils = ossUtils;
        this.cacheClient = cacheClient;
        this.venueDetailLocalCache = venueDetailLocalCache;
        this.sessionService = sessionService;
        this.sessionInventoryService = sessionInventoryService;
        this.venueGeoService = venueGeoService;
        this.geocodingService = geocodingService;
    }

    @Override
    public PageResult<CatalogVenueVO> pageSellableVenues(
            CatalogVenuePageQueryDTO queryDTO) {
        queryDTO.setKeyword(StringUtils.hasText(queryDTO.getKeyword())
                ? queryDTO.getKeyword().trim()
                : null);

        PageHelper.startPage(queryDTO.getPage(), queryDTO.getSize());
        Page<CatalogVenueVO> page = (Page<CatalogVenueVO>)
                catalogMapper.listSellableVenues(queryDTO);
        page.forEach(this::resolveCoverUrl);

        return new PageResult<>(
                page.getResult(), page.getTotal(), page.getPageNum(), page.getPageSize());
    }

    @Override
    public CatalogVenueVO getVenue(Long venueId) {
        /*
         * Cache.get() 会先查询 Caffeine。
         *
         * 如果本地缓存未命中，才执行后面的加载函数。
         * 同一个 JVM 内，同一个 venueId 并发加载时，Caffeine 会尽量避免重复加载。
         */
        CatalogVenueVO cachedVenue = venueDetailLocalCache.get(
                venueId,
                id -> cacheClient.queryWithPassThrough(
                        VenueConstant.VENUE_DETAIL_PREFIX,
                        id,
                        CatalogVenueVO.class,
                        catalogMapper::findEnabledVenue,
                        VenueConstant.VENUE_DETAIL_TTL,
                        TimeUnit.SECONDS
                )
        );

        if (cachedVenue == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "景点不存在");
        }

        /*
         * Caffeine 保存的是 objectKey。
         * 返回前复制一份对象，再把 objectKey 转换成前端可访问的 URL，
         * 避免直接修改 Caffeine 中长期保存的对象。
         */
        CatalogVenueVO result = copyVenue(cachedVenue);
        resolveCoverUrl(result);
        return result;
    }

    /**
     * 复制缓存对象，避免接口返回处理修改 Caffeine 中的原对象。
     */
    private CatalogVenueVO copyVenue(CatalogVenueVO source) {
        CatalogVenueVO target = new CatalogVenueVO();
        target.setId(source.getId());
        target.setName(source.getName());
        target.setAddress(source.getAddress());
        target.setDescription(source.getDescription());
        target.setCoverUrl(source.getCoverUrl());
        target.setMinimumPrice(source.getMinimumPrice());
        return target;
    }

    @Override
    public List<NearbyVenueVO> listNearbyVenues(
            NearbyVenueQueryDTO queryDTO) {
        return listNearbyVenues(
                queryDTO.getLongitude(),
                queryDTO.getLatitude(),
                queryDTO.getRadiusKm(),
                queryDTO.getLimit());
    }

    @Override
    public List<NearbyVenueVO> listNearbyVenuesByName(
            NearbyVenueByNameQueryDTO queryDTO) {
        GeoCoordinate coordinate = geocodingService.geocode(
                queryDTO.getName().trim(),
                StringUtils.hasText(queryDTO.getCity())
                        ? queryDTO.getCity().trim()
                        : null);

        // 名称接口只负责解析坐标，后续完全复用坐标查询逻辑。
        return listNearbyVenues(
                coordinate.longitude(),
                coordinate.latitude(),
                queryDTO.getRadiusKm(),
                queryDTO.getLimit());
    }

    private List<NearbyVenueVO> listNearbyVenues(
            double longitude,
            double latitude,
            double radiusKm,
            int limit) {
        List<VenueGeoResult> geoResults = venueGeoService.searchNearby(
                longitude,
                latitude,
                radiusKm,
                limit);

        if (geoResults.isEmpty()) {
            return List.of();
        }

        /*
         * GEO 只返回景点 ID 和距离；景点资料继续复用现有的
         * Caffeine -> Redis -> MySQL 两级缓存，并保留 GEO 的距离顺序。
         */
        return geoResults.stream()
                .map(result -> toNearbyVenue(
                        getVenue(result.venueId()),
                        result.distanceKm()))
                .toList();
    }

    private NearbyVenueVO toNearbyVenue(
            CatalogVenueVO venue,
            double distanceKm) {
        NearbyVenueVO result = new NearbyVenueVO();
        result.setId(venue.getId());
        result.setName(venue.getName());
        result.setAddress(venue.getAddress());
        result.setDescription(venue.getDescription());
        // getVenue 已经把缓存中的 objectKey 转换成可访问 URL。
        result.setCoverUrl(venue.getCoverUrl());
        result.setDistanceKm(BigDecimal.valueOf(distanceKm)
                .setScale(2, RoundingMode.HALF_UP));
        return result;
    }

    @Override
    public List<SellableSessionVO> listSellableSessions(
            Long venueId,
            LocalDate visitDate) {

        /*
         * 复用景点详情两级缓存检查景点是否存在且已启用。
         * 返回值这里不使用，只需要它完成景点有效性校验。
         */
        getVenue(venueId);

        /*
         * 当前时间只生成一次，保证本次请求中所有场次使用同一个时间基准。
         */
        LocalDateTime now = LocalDateTime.now();

        /*
         * 同时包含未开售、正在销售和已经售罄的场次。
         */
        List<Long> sessionIds =
                catalogMapper.listVisibleSessionIds(
                        venueId,
                        visitDate,
                        now
                );

        if (sessionIds.isEmpty()) {
            return List.of();
        }

        /*
         * 每个场次的静态信息经过：
         * Caffeine -> Redis -> MySQL。
         */
        List<SessionStaticSnapshotVO> staticSnapshots =
                sessionIds.stream()
                        .map(sessionService::getSessionStaticSnapshot)
                        .toList();

        /*
         * 只有已经到达bookingStartAt的场次，
         * 才需要查询动态库存。
         */
        List<Long> startedSessionIds =
                staticSnapshots.stream()
                        .filter(snapshot ->
                                !now.isBefore(
                                        snapshot.getBookingStartAt()
                                )
                        )
                        .map(
                                SessionStaticSnapshotVO::getSessionId
                        )
                        .toList();

        /*
         * 使用一次批量SQL查询所有已开售场次库存，
         * 避免每个场次单独查询造成N+1问题。
         */
        Map<Long, SessionInventorySnapshotVO> inventoryBySessionId =
                sessionInventoryService
                        .listInventorySnapshots(
                                startedSessionIds,
                                now
                        )
                        .stream()
                        .collect(Collectors.toMap(
                                SessionInventorySnapshotVO::getSessionId,
                                Function.identity()
                        ));

        /*
         * 将静态快照和可选的动态库存组装成接口响应。
         */
        return staticSnapshots.stream()
                .map(snapshot -> buildCatalogSession(
                        snapshot,
                        inventoryBySessionId.get(
                                snapshot.getSessionId()
                        ),
                        now
                ))
                .toList();
    }

    /**
     * 根据预约时间决定只返回静态信息，还是补充动态库存。
     */
    private SellableSessionVO buildCatalogSession(
            SessionStaticSnapshotVO snapshot,
            SessionInventorySnapshotVO inventory,
            LocalDateTime now) {

        SellableSessionVO result = new SellableSessionVO();

        result.setId(snapshot.getSessionId());
        result.setVisitDate(snapshot.getVisitDate());
        result.setStartTime(snapshot.getStartTime());
        result.setEndTime(snapshot.getEndTime());
        result.setBookingStartAt(snapshot.getBookingStartAt());
        result.setBookingEndAt(snapshot.getBookingEndAt());

        /*
         * 先复制静态票种信息。
         * 此时remainingQuantity保持为null。
         */
        List<SellableTicketTypeVO> ticketTypes =
                snapshot.getTicketTypes()
                        .stream()
                        .map(this::toCatalogTicketType)
                        .toList();

        result.setTicketTypes(ticketTypes);

        /*
         * 尚未开售时不查询、不返回库存。
         */
        if (now.isBefore(snapshot.getBookingStartAt())) {
            result.setSaleState(
                    CatalogSessionSaleState.NOT_STARTED
            );
            return result;
        }

        /*
         * 已开售场次正常情况下都能查到库存。
         * 如果没有动态记录，就按照库存为0展示。
         */
        int remainingCapacity =
                inventory == null
                        ? 0
                        : inventory.getRemainingCapacity();

        result.setRemainingCapacity(remainingCapacity);

        Map<Long, Integer> quantityByTicketTypeId =
                inventory == null
                        ? Map.of()
                        : inventory.getTicketTypes()
                        .stream()
                        .collect(Collectors.toMap(
                                SessionTicketTypeInventoryVO
                                        ::getSessionTicketTypeId,
                                SessionTicketTypeInventoryVO
                                        ::getRemainingQuantity
                        ));

        /*
         * 按sessionTicketTypeId把动态库存补充到静态票种上。
         */
        ticketTypes.forEach(ticketType ->
                ticketType.setRemainingQuantity(
                        quantityByTicketTypeId.getOrDefault(
                                ticketType.getSessionTicketTypeId(),
                                0
                        )
                )
        );

        boolean hasTicketInventory =
                ticketTypes.stream()
                        .anyMatch(ticketType ->
                                ticketType.getRemainingQuantity() > 0
                        );

        if (remainingCapacity > 0 && hasTicketInventory) {
            result.setSaleState(
                    CatalogSessionSaleState.ON_SALE
            );
        } else {
            result.setSaleState(
                    CatalogSessionSaleState.SOLD_OUT
            );
        }

        return result;
    }


    /**
     * 将缓存中的静态票种快照转换成游客端响应。
     */
    private SellableTicketTypeVO toCatalogTicketType(
            SessionTicketTypeStaticVO source) {

        SellableTicketTypeVO target =
                new SellableTicketTypeVO();

        target.setSessionTicketTypeId(
                source.getSessionTicketTypeId()
        );
        target.setTicketTypeName(
                source.getTicketTypeName()
        );
        target.setDescription(
                source.getDescription()
        );
        target.setAudienceRule(
                source.getAudienceRule()
        );
        target.setSalePrice(
                source.getSalePrice()
        );

        /*
         * 未开售时保持null；
         * 开售后由buildCatalogSession补充。
         */
        target.setRemainingQuantity(null);

        return target;
    }

    private void resolveCoverUrl(CatalogVenueVO venue) {
        // Mapper 暂存 objectKey，离开 Service 前统一转换成前端可访问地址。
        venue.setCoverUrl(ossUtils.getPublicUrl(venue.getCoverUrl()));
    }
}
