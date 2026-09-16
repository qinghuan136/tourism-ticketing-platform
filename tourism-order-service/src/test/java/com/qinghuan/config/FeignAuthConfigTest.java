package com.qinghuan.config;

import com.qinghuan.common.security.InternalAuthProperties;
import com.qinghuan.common.security.InternalServiceAuth;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FeignAuthConfigTest {

    private final RequestInterceptor interceptor =
            new FeignAuthConfig().authorizationForwardingInterceptor(internalAuthProperties());

    private static InternalAuthProperties internalAuthProperties() {
        InternalAuthProperties properties = new InternalAuthProperties();
        properties.setToken("internal-test-token");
        return properties;
    }

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void apply_shouldForwardOriginalAuthorizationHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer test-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertEquals(
                List.of("Bearer test-token"),
                List.copyOf(template.headers().get(HttpHeaders.AUTHORIZATION))
        );
        assertEquals(List.of("internal-test-token"),
                List.copyOf(template.headers().get(InternalServiceAuth.SERVICE_TOKEN_HEADER)));
    }

    @Test
    void apply_shouldDoNothingWithoutServletRequestContext() {
        RequestContextHolder.resetRequestAttributes();
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertNull(template.headers().get(HttpHeaders.AUTHORIZATION));
        assertEquals(List.of("internal-test-token"),
                List.copyOf(template.headers().get(InternalServiceAuth.SERVICE_TOKEN_HEADER)));
    }
}
