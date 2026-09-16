package com.qinghuan.common.jwt;

/**
 * JWT 中保存的通用身份声明，不依赖具体服务的用户上下文类型。
 */
public record JwtUser(Long userId, String loginName, String roleCode, Long venueId) {
}
