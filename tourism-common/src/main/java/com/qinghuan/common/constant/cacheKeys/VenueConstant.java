package com.qinghuan.common.constant.cacheKeys;

// 时间单位统一为second
public class VenueConstant {
    public static final String VENUE_DETAIL_PREFIX = "venue:detail:";
    /** 景点 ID 布隆过滤器；版本变化时使用新 Key 重建。 */
    public static final String VENUE_ID_BLOOM_FILTER = "bloom:venue:id:v1";
    public static final long VENUE_ID_EXPECTED_INSERTIONS = 10_000L;
    public static final double VENUE_ID_FALSE_PROBABILITY = 0.01D;
    /** 已启用景点的 Redis GEO 索引，member 使用景点 ID。 */
    public static final String VENUE_GEO_KEY = "geo:venue:enabled";
    // 景点详情缓存时间
    public static final Long VENUE_DETAIL_TTL = 60 * 60 * 24L;
}
