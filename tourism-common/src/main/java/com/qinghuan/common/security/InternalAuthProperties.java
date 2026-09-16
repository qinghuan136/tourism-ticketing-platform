package com.qinghuan.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 服务间内部接口共享认证配置。 */
@Component
@ConfigurationProperties(prefix = "internal.auth")
public class InternalAuthProperties {

    private String token = "";

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token == null ? "" : token;
    }
}
