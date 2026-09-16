package com.qinghuan.gateway.filter;

import com.qinghuan.gateway.config.GatewaySecurityProperties;
import com.qinghuan.common.jwt.JwtProperties;
import com.qinghuan.common.jwt.JwtUtils;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GatewayJwtAuthenticationFilterTest {

    private static final String SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final String ISSUER = "tourism-ticketing-platform";

    private GatewayJwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret(SECRET);
        jwtProperties.setIssuer(ISSUER);

        GatewaySecurityProperties securityProperties = new GatewaySecurityProperties();
        securityProperties.setWhitelist(List.of("/auth/login", "/public/**", "/actuator/health"));
        filter = new GatewayJwtAuthenticationFilter(new JwtUtils(jwtProperties), securityProperties);
    }

    @Test
    void shouldAllowWhitelistedPathWithoutToken() {
        MockServerWebExchange exchange = exchange("/auth/login", null);

        filter.filter(exchange, ignored -> Mono.empty()).block();

        assertNull(exchange.getResponse().getStatusCode());
    }

    @Test
    void shouldAllowHealthCheckWithoutToken() {
        MockServerWebExchange exchange = exchange("/actuator/health", null);

        filter.filter(exchange, ignored -> Mono.empty()).block();

        assertNull(exchange.getResponse().getStatusCode());
    }

    @Test
    void shouldRejectProtectedPathWithoutToken() {
        MockServerWebExchange exchange = exchange("/tourist/orders", null);

        filter.filter(exchange, ignored -> Mono.empty()).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void shouldRejectInvalidToken() {
        MockServerWebExchange exchange = exchange("/tourist/orders", "Bearer invalid-token");

        filter.filter(exchange, ignored -> Mono.empty()).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void shouldRejectExternalInternalPathBeforeJwtValidation() {
        MockServerWebExchange exchange = exchange("/internal/users/1", validAuthorization());

        filter.filter(exchange, ignored -> Mono.empty()).block();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
    }

    @Test
    void shouldKeepAuthorizationHeaderWhenTokenIsValid() {
        String authorization = validAuthorization();
        MockServerWebExchange exchange = exchange("/tourist/orders", authorization);
        AtomicReference<ServerWebExchange> forwardedExchange = new AtomicReference<>();
        GatewayFilterChain chain = forwarded -> {
            forwardedExchange.set(forwarded);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertEquals(authorization, forwardedExchange.get().getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
    }

    private MockServerWebExchange exchange(String path, String authorization) {
        MockServerHttpRequest.BaseBuilder<?> request = MockServerHttpRequest.get(path);
        if (authorization != null) {
            request.header(HttpHeaders.AUTHORIZATION, authorization);
        }
        return MockServerWebExchange.from(request);
    }

    private String validAuthorization() {
        String token = Jwts.builder()
                .issuer(ISSUER)
                .subject("1")
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                .compact();
        return "Bearer " + token;
    }
}
