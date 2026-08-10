package com.qinghuan.config.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.qinghuan.pojo.vo.CatalogVenueVO;
import com.qinghuan.pojo.vo.SessionStaticSnapshotVO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Caffeine 本地缓存配置。
 *
 * 每一种业务缓存单独定义 Bean，方便设置不同的容量和过期时间。
 */
@Configuration
public class CaffeineCacheConfig {
    private static final int venueDetailMaxSize = 500;
    private static final int sessionStaticMaxSize = venueDetailMaxSize * 5;

    /**
     * 景点详情一级缓存。
     *
     * Key：venueId
     * Value：景点详情，其中 coverUrl 暂时保存 OSS objectKey。
     */
    @Bean("venueDetailLocalCache")
    public Cache<Long, CatalogVenueVO> venueDetailLocalCache() {
        return Caffeine.newBuilder()
                .maximumSize(venueDetailMaxSize)
                // 一级缓存时间短于 Redis，减少本地脏数据持续时间。
                .expireAfterWrite(Duration.ofMinutes(10))
                // 记录命中率，后面压测时可以观察缓存效果。
                .recordStats()
                .build();
    }

    /**
     * 场次票种静态快照一级缓存。
     *
     * Key：sessionId
     * Value：场次与票种的静态展示信息
     */
    @Bean("sessionStaticLocalCache")
    public Cache<Long, SessionStaticSnapshotVO> sessionStaticLocalCache() {
        return Caffeine.newBuilder()
                // 场次数量通常高于景点数量，因此容量比景点缓存稍大。
                .maximumSize(sessionStaticMaxSize)
                // 一级缓存保持较短时间，Redis继续作为容量更大的二级缓存。
                .expireAfterWrite(Duration.ofMinutes(15))
                .recordStats()
                .build();
    }
}