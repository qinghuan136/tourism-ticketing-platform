package com.qinghuan.cache;

import cn.hutool.json.JSONUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.qinghuan.pojo.vo.CatalogVenueVO;
import com.qinghuan.pojo.vo.SessionStaticSnapshotVO;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 每个 Spring Boot 实例都会订阅该消息。
 *
 * 收到消息后，只删除当前 JVM 中的 Caffeine 缓存。
 * Redis 二级缓存已经由 Canal 消费端统一删除。
 */
@Slf4j
@Component
public class CacheInvalidationSubscriber
        implements MessageListener {

    private final Cache<Long, CatalogVenueVO>
            venueDetailLocalCache;

    private final Cache<Long, SessionStaticSnapshotVO>
            sessionStaticLocalCache;

    public CacheInvalidationSubscriber(
            @Qualifier("venueDetailLocalCache")
            Cache<Long, CatalogVenueVO> venueDetailLocalCache,

            @Qualifier("sessionStaticLocalCache")
            Cache<Long, SessionStaticSnapshotVO>
                    sessionStaticLocalCache) {

        this.venueDetailLocalCache =
                venueDetailLocalCache;
        this.sessionStaticLocalCache =
                sessionStaticLocalCache;
    }

    @Override
    public void onMessage(
            Message redisMessage,
            byte[] pattern) {

        String json = new String(
                redisMessage.getBody(),
                StandardCharsets.UTF_8
        );

        CacheInvalidationMessage message =
                JSONUtil.toBean(
                        json,
                        CacheInvalidationMessage.class
                );

        switch (message.getCacheName()) {
            case CacheInvalidationConstant.VENUE_DETAIL ->
                    venueDetailLocalCache.invalidate(
                            message.getId()
                    );

            case CacheInvalidationConstant.SESSION_STATIC ->
                    sessionStaticLocalCache.invalidate(
                            message.getId()
                    );

            default -> log.warn(
                    "收到未知缓存失效消息：{}",
                    json
            );
        }

        log.info(
                "本地缓存已失效，cacheName={}，id={}",
                message.getCacheName(),
                message.getId()
        );
    }
}