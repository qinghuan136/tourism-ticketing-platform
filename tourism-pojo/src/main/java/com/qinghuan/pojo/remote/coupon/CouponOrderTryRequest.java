package com.qinghuan.pojo.remote.coupon;

import java.math.BigDecimal;

/** 订单服务向优惠券服务发起的锁券请求。 */
public record CouponOrderTryRequest(
        Long orderId,
        Long couponId,
        Long userId,
        Long venueId,
        BigDecimal originalAmount) {
}
