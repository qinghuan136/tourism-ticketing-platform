package com.qinghuan.booking.refund;

/**
 * 退款平台边界。
 * 后续接入真实支付平台时，只需替换该接口实现。
 */
public interface RefundGateway {

    RefundGatewayResult requestRefund(RefundRequest request);

    RefundGatewayResult queryRefund(String refundNo);
}
