package com.qinghuan.cache;

import com.qinghuan.common.constant.cacheKeys.SessionCacheConstant;
import com.qinghuan.common.constant.cacheKeys.VenueConstant;
import com.qinghuan.redis.CacheClient;
import org.springframework.stereotype.Component;

/**
 * Canal 检测到数据库变化后，通过该组件发起分布式缓存失效。
 *
 * 顺序必须是：
 * 1. 删除 Redis
 * 2. 广播 Caffeine 失效消息
 */
@Component
public class DistributedCacheInvalidator {

    private final CacheClient cacheClient;
    private final CacheInvalidationPublisher publisher;

    public DistributedCacheInvalidator(
            CacheClient cacheClient,
            CacheInvalidationPublisher publisher) {
        this.cacheClient = cacheClient;
        this.publisher = publisher;
    }

    public void invalidateVenueDetail(Long venueId) {
        cacheClient.delete(
                VenueConstant.VENUE_DETAIL_PREFIX + venueId
        );

        publisher.publish(
                CacheInvalidationConstant.VENUE_DETAIL,
                venueId
        );
    }

    public void invalidateSessionStatic(Long sessionId) {
        cacheClient.delete(
                SessionCacheConstant.SESSION_STATIC_PREFIX + sessionId
        );

        publisher.publish(
                CacheInvalidationConstant.SESSION_STATIC,
                sessionId
        );
    }
}