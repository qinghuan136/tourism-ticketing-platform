package com.qinghuan.pojo.remote.coupon;

import java.math.BigDecimal;

/** 锁券成功后返回的优惠金额快照。 */
public record CouponOrderTryResponse(
        Long couponId,
        BigDecimal discountAmount,
        BigDecimal payableAmount) {
}
