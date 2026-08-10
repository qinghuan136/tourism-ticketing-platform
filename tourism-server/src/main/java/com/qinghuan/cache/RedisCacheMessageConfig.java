package com.qinghuan.config.cache;

import com.qinghuan.cache.CacheInvalidationConstant;
import com.qinghuan.cache.CacheInvalidationSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Redis Pub/Sub 订阅配置。
 */
@Configuration
public class RedisCacheMessageConfig {

    @Bean
    public RedisMessageListenerContainer
    cacheInvalidationListenerContainer(
            RedisConnectionFactory connectionFactory,
            CacheInvalidationSubscriber subscriber) {

        RedisMessageListenerContainer container =
                new RedisMessageListenerContainer();

        container.setConnectionFactory(connectionFactory);

        container.addMessageListener(
                subscriber,
                new ChannelTopic(
                        CacheInvalidationConstant.CHANNEL
                )
        );

        return container;
    }
}