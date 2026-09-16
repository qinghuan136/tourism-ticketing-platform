package com.qinghuan.pojo.entity;

import com.qinghuan.pojo.enums.CouponOrderReservationStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 优惠券服务侧的 Try/Confirm/Cancel 幂等记录。 */
@Getter
@Setter
public class CouponOrderReservation {
    private Long orderId;
    private Long couponId;
    private CouponOrderReservationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
