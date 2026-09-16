package com.qinghuan.coupon;

import java.math.BigDecimal;

/** 订单锁券后得到的优惠金额计算结果。 */
public record CouponDiscount(Long couponId,
                             BigDecimal discountAmount,
                             BigDecimal payableAmount) {
}
