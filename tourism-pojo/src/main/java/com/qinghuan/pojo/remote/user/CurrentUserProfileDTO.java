package com.qinghuan.pojo.remote.user;

/** 订单创建时由用户服务提供的购买人展示快照。 */
public record CurrentUserProfileDTO(String displayName, String phone) {
}
