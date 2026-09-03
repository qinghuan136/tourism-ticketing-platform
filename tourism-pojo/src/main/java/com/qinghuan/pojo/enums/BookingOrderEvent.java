package com.qinghuan.pojo.enums;

/**
 * 驱动订单状态变化的业务事件。
 */
public enum BookingOrderEvent {
    PAY_SUCCESS,
    USER_CANCEL,
    PAYMENT_TIMEOUT,
    REFUND_REQUESTED,
    REFUND_SUCCESS,
    REFUND_FAILED,
    FULFILLMENT_FINISHED
}
