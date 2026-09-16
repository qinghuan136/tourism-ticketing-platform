package com.qinghuan.session;

import com.qinghuan.common.security.InternalAuthProperties;
import com.qinghuan.common.security.InternalServiceAuth;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;

/** 仅向内部 API 透传服务令牌；此调用不依赖游客 HTTP 上下文。 */
public class ServiceTokenFeignConfig {

    @Bean
    RequestInterceptor serviceTokenInterceptor(InternalAuthProperties properties) {
        return template -> {
            if (!properties.getToken().isBlank()) {
                template.header(InternalServiceAuth.SERVICE_TOKEN_HEADER, properties.getToken());
            }
        };
    }
}
