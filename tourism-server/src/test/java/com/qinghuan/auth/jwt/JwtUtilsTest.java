package com.qinghuan.auth.jwt;

import com.qinghuan.auth.config.JwtProperties;
import com.qinghuan.auth.model.LoginUser;
import com.qinghuan.pojo.enums.AccountRole;
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
    @DisplayName("签发的令牌应还原完整登录身份")
    void parseAccessToken_shouldRestoreLoginUser_whenTokenIsValid() {
        LoginUser original = new LoginUser(12L, "operator-01", AccountRole.OPERATOR, 7L);

        LoginUser parsed = jwtUtils.parseAccessToken(jwtUtils.generateAccessToken(original));

        assertEquals(original, parsed);
    }

    @Test
    @DisplayName("签名被篡改的令牌应被拒绝")
    void parseAccessToken_shouldRejectToken_whenSignatureIsTampered() {
        String token = jwtUtils.generateAccessToken(new LoginUser(12L, "tourist-01", AccountRole.TOURIST, null));

        assertThrows(JwtException.class, () -> jwtUtils.parseAccessToken(token + "tampered"));
    }

    @Test
    @DisplayName("已过期令牌应被拒绝")
    void parseAccessToken_shouldRejectToken_whenExpired() {
        JwtUtils expiredTokenUtils = new JwtUtils(
                properties,
                Clock.fixed(Instant.now().minus(Duration.ofHours(3)), ZoneOffset.UTC)
        );
        String token = expiredTokenUtils.generateAccessToken(new LoginUser(12L, "staff-01", AccountRole.STAFF, 7L));

        assertThrows(ExpiredJwtException.class, () -> expiredTokenUtils.parseAccessToken(token));
    }

    @Test
    @DisplayName("没有所属场馆的用户应保留空场馆标识")
    void parseAccessToken_shouldKeepVenueIdNull_whenVenueIdIsAbsent() {
        LoginUser parsed = jwtUtils.parseAccessToken(
                jwtUtils.generateAccessToken(new LoginUser(1L, "admin", AccountRole.ADMIN, null))
        );

        assertAll(
                () -> assertEquals(AccountRole.ADMIN, parsed.roleCode()),
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
