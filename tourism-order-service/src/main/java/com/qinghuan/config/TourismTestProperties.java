package com.qinghuan.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/** 用于确认 Nacos Config 动态刷新的最小配置项。 */
@Component
@RefreshScope
@ConfigurationProperties(prefix = "tourism.test")
public class TourismTestProperties {

    private int value = 10;

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
