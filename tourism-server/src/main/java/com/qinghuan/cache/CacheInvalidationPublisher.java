package com.qinghuan.cache;

import cn.hutool.json.JSONUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 向所有业务实例广播缓存失效消息。
 */
@Component
public class CacheInvalidationPublisher {

    private final StringRedisTemplate stringRedisTemplate;

    public CacheInvalidationPublisher(
            StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void publish(String cacheName, Long id) {
        CacheInvalidationMessage message =
                new CacheInvalidationMessage(cacheName, id);

        stringRedisTemplate.convertAndSend(
                CacheInvalidationConstant.CHANNEL,
                JSONUtil.toJsonStr(message)
        );
    }
}