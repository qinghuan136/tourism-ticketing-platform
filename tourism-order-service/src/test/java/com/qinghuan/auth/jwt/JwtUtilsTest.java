package com.qinghuan.auth.jwt;

import com.qinghuan.common.jwt.JwtProperties;
import com.qinghuan.common.jwt.JwtUser;
import com.qinghuan.common.jwt.JwtUtils;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("JWT 工具类")
class JwtUtilsTest {

    private JwtProperties properties;
    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        properties = testProperties();
        jwtUtils = new JwtUtils(properties);
    }

    @Test
    @DisplayName("签发的令牌应还原完整身份声明")
    void parseAccessToken_shouldRestoreJwtUser_whenTokenIsValid() {
        JwtUser original = new JwtUser(12L, "operator-01", "OPERATOR", 7L);

        JwtUser parsed = jwtUtils.parseAccessToken(jwtUtils.generateAccessToken(original));

        assertEquals(original, parsed);
    }

    @Test
    @DisplayName("签名被篡改的令牌应被拒绝")
    void parseAccessToken_shouldRejectToken_whenSignatureIsTampered() {
        String token = jwtUtils.generateAccessToken(new JwtUser(12L, "tourist-01", "TOURIST", null));

        assertThrows(JwtException.class, () -> jwtUtils.parseAccessToken(token + "tampered"));
    }

    @Test
    @DisplayName("已过期令牌应被拒绝")
    void parseAccessToken_shouldRejectToken_whenExpired() {
        JwtUtils expiredTokenUtils = new JwtUtils(
                properties,
                Clock.fixed(Instant.now().minus(Duration.ofHours(3)), ZoneOffset.UTC)
        );
        String token = expiredTokenUtils.generateAccessToken(new JwtUser(12L, "staff-01", "STAFF", 7L));

        assertThrows(ExpiredJwtException.class, () -> expiredTokenUtils.parseAccessToken(token));
    }

    @Test
    @DisplayName("没有所属场馆的用户应保留空场馆标识")
    void parseAccessToken_shouldKeepVenueIdNull_whenVenueIdIsAbsent() {
        JwtUser parsed = jwtUtils.parseAccessToken(
                jwtUtils.generateAccessToken(new JwtUser(1L, "admin", "ADMIN", null))
        );

        assertAll(
                () -> assertEquals("ADMIN", parsed.roleCode()),
                () -> assertNull(parsed.venueId())
        );
    }

    private static JwtProperties testProperties() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        properties.setIssuer("tourism-ticketing-platform-test");
        properties.setAccessTokenTtl(Duration.ofHours(2));
        return properties;
    }
}
