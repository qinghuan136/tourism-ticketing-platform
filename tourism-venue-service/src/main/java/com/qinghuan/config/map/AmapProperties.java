package com.qinghuan.config.map;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 高德 Web 服务配置，Key 通过环境变量提供。 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.amap")
public class AmapProperties {

    private String baseUrl;
    private String key;
}
