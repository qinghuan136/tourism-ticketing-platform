package com.qinghuan.auth.context;

import com.qinghuan.auth.model.LoginUser;
import com.qinghuan.common.exception.BusinessException;
import com.qinghuan.common.exception.ErrorCode;

import java.util.Objects;
import java.util.Optional;

/**
 * 当前 HTTP 请求的登录用户上下文。必须由 Filter 在请求结束时清理。
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> CURRENT_USER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser loginUser) {
        CURRENT_USER.set(Objects.requireNonNull(loginUser, "loginUser must not be null"));
    }

    public static Optional<LoginUser> get() {
        return Optional.ofNullable(CURRENT_USER.get());
    }

    public static LoginUser getRequired() {
        return get().orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    public static Long getUserId() {
        return getRequired().userId();
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
