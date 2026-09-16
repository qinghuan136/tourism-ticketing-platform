package com.qinghuan.coupon;

import com.qinghuan.common.security.InternalAuthProperties;
import com.qinghuan.common.security.InternalServiceAuth;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;

/** 内部景点查询只携带服务令牌，不依赖 Kafka 或定时任务的请求上下文。 */
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
