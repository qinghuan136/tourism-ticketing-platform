package com.qinghuan.gateway.filter;

import com.qinghuan.common.jwt.JwtUtils;
import com.qinghuan.gateway.config.GatewaySecurityProperties;
import io.jsonwebtoken.JwtException;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 在路由前验证外部请求携带的 JWT，原 Authorization 请求头不作改写。
 */
@Component
public class GatewayJwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";
    private final JwtUtils jwtUtils;
    private final GatewaySecurityProperties securityProperties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public GatewayJwtAuthenticationFilter(
            JwtUtils jwtUtils,
            GatewaySecurityProperties securityProperties) {
        this.jwtUtils = jwtUtils;
        this.securityProperties = securityProperties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        if (isInternalPath(path)) {
            return complete(exchange.getResponse(), HttpStatus.FORBIDDEN);
        }
        if (isWhitelisted(path)) {
            return chain.filter(exchange);
        }

        try {
            String token = resolveBearerToken(exchange);
            if (token == null) {
                return complete(exchange.getResponse(), HttpStatus.UNAUTHORIZED);
            }
            jwtUtils.parseAccessToken(token);
            return chain.filter(exchange);
        } catch (JwtException | IllegalArgumentException exception) {
            return complete(exchange.getResponse(), HttpStatus.UNAUTHORIZED);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private boolean isInternalPath(String path) {
        return "/internal".equals(path) || pathMatcher.match("/internal/**", path);
    }

    private boolean isWhitelisted(String path) {
        return securityProperties.getWhitelist().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private String resolveBearerToken(ServerWebExchange exchange) {
        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null;
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }

    private Mono<Void> complete(ServerHttpResponse response, HttpStatus status) {
        response.setStatusCode(status);
        return response.setComplete();
    }
}
