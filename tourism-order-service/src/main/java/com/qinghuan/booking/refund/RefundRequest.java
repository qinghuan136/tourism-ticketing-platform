package com.qinghuan.booking.refund;

import java.math.BigDecimal;

/** 发送给退款平台的必要订单快照。 */
public record RefundRequest(
        Long orderId,
        String refundNo,
        String paymentNo,
        BigDecimal amount) {
}
