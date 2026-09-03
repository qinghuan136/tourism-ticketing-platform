package com.qinghuan.redis;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
class RedisConnectionTest {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void testConnection() {
        stringRedisTemplate.opsForValue()
                .set("test:name", "qinghuan");

        String value = stringRedisTemplate.opsForValue()
                .get("test:name");

        System.out.println(value);
    }
}