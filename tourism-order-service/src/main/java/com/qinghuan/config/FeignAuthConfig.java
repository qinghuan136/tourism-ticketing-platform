package com.qinghuan.config;

import com.qinghuan.common.exception.BusinessException;
import com.qinghuan.common.exception.ErrorCode;
import com.qinghuan.common.security.InternalAuthProperties;
import com.qinghuan.common.security.InternalServiceAuth;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Arrays;

/** 透传进入订单服务的 JWT，使下游服务自行完成认证。 */
@Configuration
public class FeignAuthConfig {

    @Bean
    public RequestInterceptor authorizationForwardingInterceptor(InternalAuthProperties internalAuthProperties) {
        return requestTemplate -> {
            // 服务间调用始终携带内部令牌；下游仍会继续验证原始 JWT。
            if (!internalAuthProperties.getToken().isBlank()) {
                requestTemplate.header(InternalServiceAuth.SERVICE_TOKEN_HEADER,
                        internalAuthProperties.getToken());
            }
            // 定时任务、Kafka 消费者等调用链没有 Servlet 请求上下文，不传递认证头。
            if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
                return;
            }

            HttpServletRequest request = attributes.getRequest();
            String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authorization != null && !authorization.isBlank()) {
                requestTemplate.header(HttpHeaders.AUTHORIZATION, authorization);
            }
        };
    }

    /** 将下游统一错误响应还原为本地业务异常，保留原有的 HTTP 错误语义。 */
    @Bean
    public ErrorDecoder feignBusinessErrorDecoder(ObjectMapper objectMapper) {
        ErrorDecoder fallback = new ErrorDecoder.Default();
        return (methodKey, response) -> {
            if (response.body() == null) {
                return fallback.decode(methodKey, response);
            }
            try {
                RemoteErrorBody body = objectMapper.readValue(
                        response.body().asInputStream(), RemoteErrorBody.class);
                ErrorCode errorCode = Arrays.stream(ErrorCode.values())
                        .filter(value -> value.getCode().equals(body.code()))
                        .findFirst()
                        .orElse(ErrorCode.INTERNAL_ERROR);
                return new BusinessException(errorCode, body.message());
            } catch (IOException exception) {
                return fallback.decode(methodKey, response);
            }
        };
    }

    private record RemoteErrorBody(String code, String message) {
    }
}
