package com.qinghuan.common.constant.cacheKeys;

// 时间单位统一为second
public class VenueConstant {
    public static final String VENUE_DETAIL_PREFIX = "venue:detail:";
    /** 已启用景点的 Redis GEO 索引，member 使用景点 ID。 */
    public static final String VENUE_GEO_KEY = "geo:venue:enabled";
    // 景点详情缓存时间
    public static final Long VENUE_DETAIL_TTL = 60 * 60 * 24L;
}
