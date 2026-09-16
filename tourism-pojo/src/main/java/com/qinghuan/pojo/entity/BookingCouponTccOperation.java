package com.qinghuan.pojo.entity;

import com.qinghuan.pojo.enums.BookingCouponTccStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 订单创建期间优惠券远程操作的持久化协调记录。 */
@Getter
@Setter
public class BookingCouponTccOperation {
    private Long orderId;
    private Long couponId;
    private BookingCouponTccStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
