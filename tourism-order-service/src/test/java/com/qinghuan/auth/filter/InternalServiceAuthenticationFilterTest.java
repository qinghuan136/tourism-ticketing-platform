package com.qinghuan.auth.filter;

import com.qinghuan.common.security.InternalAuthProperties;
import com.qinghuan.common.security.InternalServiceAuth;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class InternalServiceAuthenticationFilterTest {

    @Test
    void shouldRejectInternalRequestWithoutServiceToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internal/coupons/confirm");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter().doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
    }

    @Test
    void shouldAllowInternalRequestWithServiceToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internal/coupons/confirm");
        request.addHeader(InternalServiceAuth.SERVICE_TOKEN_HEADER, "internal-test-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter().doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    private InternalServiceAuthenticationFilter filter() {
        InternalAuthProperties properties = new InternalAuthProperties();
        properties.setToken("internal-test-token");
        return new InternalServiceAuthenticationFilter(properties, new ObjectMapper());
    }
}
