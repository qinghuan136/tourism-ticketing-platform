package com.qinghuan.auth.filter;

import com.qinghuan.auth.config.JwtProperties;
import com.qinghuan.auth.context.UserContext;
import com.qinghuan.auth.jwt.JwtUtils;
import com.qinghuan.auth.model.LoginUser;
import com.qinghuan.common.constant.cacheKeys.AccountConstant;
import com.qinghuan.common.exception.ErrorCode;
import com.qinghuan.common.response.ApiResponse;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 在请求进入 Controller 前校验 Bearer Access Token，并建立当前用户上下文。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtils jwtUtils;
    private final JwtProperties jwtProperties;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtAuthenticationFilter(
            JwtUtils jwtUtils,
            JwtProperties jwtProperties,
            ObjectMapper objectMapper,
            StringRedisTemplate stringRedisTemplate) {
        this.jwtUtils = jwtUtils;
        this.jwtProperties = jwtProperties;
        this.objectMapper = objectMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return jwtProperties.getExcludedPaths().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        LoginUser loginUser;
        try {
            String token = resolveBearerToken(request);
            if (token == null) {
                writeUnauthorized(response, "请先登录");
                return;
            }
            loginUser = jwtUtils.parseAccessToken(token);
        } catch (JwtException | IllegalArgumentException exception) {
            writeUnauthorized(response, "登录状态已失效，请重新登录");
            return;
        }

        // 停用账号写入 Redis 后，已经签发的 JWT 也立即失效。
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(
                AccountConstant.disabledUserKey(loginUser.userId())))) {
            writeUnauthorized(response, "账号已停用");
            return;
        }

        UserContext.set(loginUser);
        try {
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null;
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.failure(ErrorCode.UNAUTHORIZED, message));
    }
}
