package com.qinghuan.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/** 以日志记录配置加载与动态刷新结果，避免增加仅用于验证的 HTTP 接口。 */
@Slf4j
@Configuration
public class TourismTestConfigLogger {

    private final TourismTestProperties properties;

    public TourismTestConfigLogger(TourismTestProperties properties) {
        this.properties = properties;
    }

    @Bean
    ApplicationRunner logInitialTestValue() {
        return args -> log.info("Nacos 配置已加载，tourism.test.value={}", properties.getValue());
    }

    @EventListener(EnvironmentChangeEvent.class)
    public void logRefreshedTestValue(EnvironmentChangeEvent event) {
        if (event.getKeys().contains("tourism.test.value")) {
            log.info("Nacos 配置已刷新，tourism.test.value={}", properties.getValue());
        }
    }
}
