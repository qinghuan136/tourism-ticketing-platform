package com.qinghuan.cache;

/**
 * 分布式缓存失效消息常量。
 */
public final class CacheInvalidationConstant {

    private CacheInvalidationConstant() {
    }


    public static final String CHANNEL =
            "tourism:cache:invalidate";

    public static final String VENUE_DETAIL =
            "VENUE_DETAIL";

    public static final String SESSION_STATIC =
            "SESSION_STATIC";
}