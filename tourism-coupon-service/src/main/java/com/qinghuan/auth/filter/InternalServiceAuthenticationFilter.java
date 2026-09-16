package com.qinghuan.auth.filter;

import com.qinghuan.common.exception.ErrorCode;
import com.qinghuan.common.response.ApiResponse;
import com.qinghuan.common.security.InternalAuthProperties;
import com.qinghuan.common.security.InternalServiceAuth;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** 保护仅供服务间调用的 /internal/** 接口。 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class InternalServiceAuthenticationFilter extends OncePerRequestFilter {
    private final InternalAuthProperties properties;
    private final ObjectMapper objectMapper;

    public InternalServiceAuthenticationFilter(InternalAuthProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !path.startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (properties.getToken().isBlank()
                || !properties.getToken().equals(request.getHeader(InternalServiceAuth.SERVICE_TOKEN_HEADER))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), ApiResponse.failure(ErrorCode.FORBIDDEN, "无权访问内部接口"));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
