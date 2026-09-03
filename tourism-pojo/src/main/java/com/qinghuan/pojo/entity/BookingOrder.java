package com.qinghuan.pojo.entity;

import com.qinghuan.pojo.enums.BookingOrderStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class BookingOrder extends BaseEntity {

    private String orderNo;
    private Long userId;
    private Long sessionId;
    private Integer quantity;
    /** 本单使用的游客优惠券；为空表示未使用优惠券。 */
    private Long userCouponId;
    /** 订单明细价格之和，即优惠前金额。 */
    private BigDecimal originalAmount;
    /** 下单时保存的优惠金额快照。 */
    private BigDecimal discountAmount;
    /** 游客最终应付金额。 */
    private BigDecimal totalAmount;
    private BookingOrderStatus status;
    private String paymentNo;
    /** 同一次退款查询和重试始终使用同一个单号。 */
    private String refundNo;
    private LocalDateTime expireAt;
    private LocalDateTime paidAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime closedAt;
    private LocalDateTime completedAt;
    private LocalDateTime refundRequestedAt;
    private LocalDateTime refundAt;
}
