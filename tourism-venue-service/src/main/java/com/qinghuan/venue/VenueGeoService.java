package com.qinghuan.venue;

import java.util.List;

public interface VenueGeoService {

    /** 根据数据库中的启用景点重建 GEO 索引。 */
    void rebuild();

    /** 景点资料变化后同步单个 GEO member。 */
    void syncVenue(Long venueId);

    /** 按距离由近到远查询指定半径内的景点。 */
    List<VenueGeoResult> searchNearby(
            double longitude,
            double latitude,
            double radiusKm,
            int limit);
}
