package com.qinghuan.auth.model;

import com.qinghuan.pojo.enums.AccountRole;

/**
 * 已认证用户在单次请求中需要的最小身份信息。
 */
public record LoginUser(Long userId, String loginName, AccountRole roleCode, Long venueId) {

    public LoginUser {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
        if (loginName == null || loginName.isBlank()) {
            throw new IllegalArgumentException("loginName must not be blank");
        }
        if (roleCode == null) {
            throw new IllegalArgumentException("roleCode must not be null");
        }
    }
}
