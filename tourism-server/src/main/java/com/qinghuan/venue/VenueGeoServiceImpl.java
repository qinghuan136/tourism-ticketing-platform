package com.qinghuan.venue;

import com.qinghuan.common.constant.cacheKeys.VenueConstant;
import com.qinghuan.pojo.entity.Venue;
import com.qinghuan.pojo.enums.VenueStatus;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoSearchCommandArgs;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class VenueGeoServiceImpl implements VenueGeoService {

    private final StringRedisTemplate stringRedisTemplate;
    private final VenueMapper venueMapper;

    public VenueGeoServiceImpl(
            StringRedisTemplate stringRedisTemplate,
            VenueMapper venueMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.venueMapper = venueMapper;
    }

    @Override
    public void rebuild() {
        List<Venue> venues = venueMapper.listEnabledVenueCoordinates();

        // GEO 是可重建索引，启动时以 MySQL 数据为准重新生成。
        stringRedisTemplate.delete(VenueConstant.VENUE_GEO_KEY);
        if (venues.isEmpty()) {
            return;
        }

        Map<String, Point> locations = venues.stream()
                .collect(Collectors.toMap(
                        venue -> venue.getId().toString(),
                        venue -> new Point(
                                venue.getLongitude().doubleValue(),
                                venue.getLatitude().doubleValue())
                ));

        stringRedisTemplate.opsForGeo()
                .add(VenueConstant.VENUE_GEO_KEY, locations);
    }

    @Override
    public void syncVenue(Long venueId) {
        String member = venueId.toString();
        Venue venue = venueMapper.getVenueById(venueId);

        /*
         * 先移除旧 member，既能覆盖坐标，也能处理景点被停用或删除。
         * GEOADD 对同一个 member 本身也是幂等的。
         */
        stringRedisTemplate.opsForGeo()
                .remove(VenueConstant.VENUE_GEO_KEY, member);

        if (venue == null
                || venue.getStatus() != VenueStatus.ENABLED
                || venue.getLongitude() == null
                || venue.getLatitude() == null) {
            return;
        }

        Point point = new Point(
                venue.getLongitude().doubleValue(),
                venue.getLatitude().doubleValue());

        stringRedisTemplate.opsForGeo()
                .add(VenueConstant.VENUE_GEO_KEY, point, member);
    }

    @Override
    public List<VenueGeoResult> searchNearby(
            double longitude,
            double latitude,
            double radiusKm,
            int limit) {

        GeoReference<String> center =
                GeoReference.fromCoordinate(longitude, latitude);
        Distance radius = new Distance(radiusKm, Metrics.KILOMETERS);
        GeoSearchCommandArgs args = GeoSearchCommandArgs.newGeoSearchArgs()
                .includeDistance()
                .sortAscending()
                .limit(limit);

        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                stringRedisTemplate.opsForGeo().search(
                        VenueConstant.VENUE_GEO_KEY,
                        center,
                        radius,
                        args);

        if (results == null) {
            return List.of();
        }

        // Redis 已经按距离升序排列，后续组装响应时必须保留该顺序。
        return results.getContent().stream()
                .map(result -> new VenueGeoResult(
                        Long.valueOf(result.getContent().getName()),
                        result.getDistance().getValue()))
                .toList();
    }
}
