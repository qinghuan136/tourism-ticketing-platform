package com.qinghuan.auth.filter;

import com.qinghuan.auth.context.UserContext;
import com.qinghuan.auth.model.LoginUser;
import com.qinghuan.common.constant.cacheKeys.AccountConstant;
import com.qinghuan.common.jwt.JwtProperties;
import com.qinghuan.common.jwt.JwtUser;
import com.qinghuan.common.jwt.JwtUtils;
import com.qinghuan.pojo.enums.AccountRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("JWT 认证过滤器")
class JwtAuthenticationFilterTest {

    private JwtUtils jwtUtils;
    private JwtAuthenticationFilter filter;
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        properties.setIssuer("tourism-ticketing-platform-test");
        properties.setAccessTokenTtl(Duration.ofHours(2));
        properties.setExcludedPaths(List.of("/auth/login", "/public/**", "/swagger-ui/**"));
        jwtUtils = new JwtUtils(properties);
        stringRedisTemplate = mock(StringRedisTemplate.class);
        filter = new JwtAuthenticationFilter(
                jwtUtils, properties, new ObjectMapper(), stringRedisTemplate);
    }

    @AfterEach
    void clearContext() {
        UserContext.clear();
    }

    @Test
    @DisplayName("有效令牌应在调用链中建立上下文并在结束后清理")
    void doFilter_shouldSetAndClearContext_whenTokenIsValid() throws Exception {
        LoginUser expected = new LoginUser(9L, "operator-01", AccountRole.OPERATOR, 3L);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/operator/staff");
        request.addHeader("Authorization", "Bearer " + token(expected));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<LoginUser> userSeenByChain = new AtomicReference<>();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                userSeenByChain.set(UserContext.getRequired())
        );

        assertAll(
                () -> assertEquals(expected, userSeenByChain.get()),
                () -> assertTrue(UserContext.get().isEmpty())
        );
    }

    @Test
    @DisplayName("受保护路径缺少令牌应返回 401 且不进入调用链")
    void doFilter_shouldReturnUnauthorized_whenTokenIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/operator/staff");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        filter.doFilter(request, response, (servletRequest, servletResponse) -> chainInvoked.set(true));

        assertAll(
                () -> assertEquals(401, response.getStatus()),
                () -> assertFalse(chainInvoked.get()),
                () -> assertTrue(response.getContentAsString().contains("UNAUTHORIZED"))
        );
    }

    @Test
    @DisplayName("公开路径不应要求令牌")
    void doFilter_shouldSkipAuthentication_whenPathIsExcluded() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        filter.doFilter(request, response, (servletRequest, servletResponse) -> chainInvoked.set(true));

        assertTrue(chainInvoked.get());
    }

    @Test
    @DisplayName("游客端公开查询路径不应要求令牌")
    void doFilter_shouldSkipAuthenticationForPublicCatalog() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/public/venues/10/sessions");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        filter.doFilter(request, response,
                (servletRequest, servletResponse) -> chainInvoked.set(true));

        assertTrue(chainInvoked.get());
    }

    @Test
    @DisplayName("停用账号的旧令牌应立即失效")
    void doFilter_shouldRejectToken_whenAccountIsDisabled() throws Exception {
        LoginUser loginUser = new LoginUser(9L, "staff-01", AccountRole.STAFF, 3L);
        when(stringRedisTemplate.hasKey(AccountConstant.disabledUserKey(9L))).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/operator/orders");
        request.addHeader("Authorization", "Bearer " + token(loginUser));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        filter.doFilter(request, response,
                (servletRequest, servletResponse) -> chainInvoked.set(true));

        assertAll(
                () -> assertEquals(401, response.getStatus()),
                () -> assertFalse(chainInvoked.get()),
                () -> assertTrue(response.getContentAsString().contains("账号已停用"))
        );
    }

    @Test
    @DisplayName("下游业务异常不应被误判为令牌失效")
    void doFilter_shouldPropagateDownstreamIllegalArgumentException() {
        LoginUser loginUser = new LoginUser(9L, "operator-01", AccountRole.OPERATOR, 3L);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/operator/venue");
        request.addHeader("Authorization", "Bearer " + token(loginUser));
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThrows(
                IllegalArgumentException.class,
                () -> filter.doFilter(request, response, (servletRequest, servletResponse) -> {
                    throw new IllegalArgumentException("业务参数错误");
                })
        );

        assertAll(
                () -> assertEquals(200, response.getStatus()),
                () -> assertTrue(UserContext.get().isEmpty())
        );
    }

    private String token(LoginUser loginUser) {
        return jwtUtils.generateAccessToken(new JwtUser(
                loginUser.userId(), loginUser.loginName(), loginUser.roleCode().name(), loginUser.venueId()));
    }
}
