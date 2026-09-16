package com.qinghuan.venue;

/** Redis GEO 返回的景点 ID 和距离。 */
public record VenueGeoResult(Long venueId, double distanceKm) {
}
