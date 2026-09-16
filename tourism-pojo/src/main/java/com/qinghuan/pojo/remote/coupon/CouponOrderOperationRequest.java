package com.qinghuan.pojo.remote.coupon;

import java.time.LocalDateTime;

/** Confirm、Cancel 和退款恢复共用的订单业务键。 */
public record CouponOrderOperationRequest(
        Long orderId,
        Long couponId,
        LocalDateTime operatedAt) {
}
