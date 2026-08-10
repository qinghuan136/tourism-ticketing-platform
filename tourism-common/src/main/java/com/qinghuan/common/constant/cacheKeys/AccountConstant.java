package com.qinghuan.common.constant.cacheKeys;

/** 账号即时失效使用的 Redis Key。 */
public final class AccountConstant {

    private static final String DISABLED_USER_PREFIX = "security:disabled-user:";

    private AccountConstant() {
    }

    public static String disabledUserKey(Long userId) {
        return DISABLED_USER_PREFIX + userId;
    }
}
