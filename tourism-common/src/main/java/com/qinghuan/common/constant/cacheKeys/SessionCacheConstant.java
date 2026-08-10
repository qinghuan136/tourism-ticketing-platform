package com.qinghuan.common.constant.cacheKeys;

/**
 * 场次相关缓存Key。
 */
public class SessionCacheConstant {

    /**
     * 场次票种静态快照。
     *
     * 完整Key示例：
     * session:ticket-types:static:21
     */
    public static final String SESSION_STATIC_PREFIX =
            "session:ticket-types:static:";

    /** Redis静态快照缓存24小时。 */
    public static final Long SESSION_STATIC_TTL = 60 * 60 * 24L;
}