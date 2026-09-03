package com.qinghuan.pojo.enums;

public enum BookingOrderStatus {
    PENDING_PAYMENT,
    PAID,
    REFUNDING,
    CANCELLED,
    CLOSED,
    COMPLETED,
    REFUNDED;

    /**
     * 根据当前状态和业务事件计算下一状态，未声明的组合均为非法流转。
     */
    public BookingOrderStatus next(BookingOrderEvent event) {
        return switch (this) {
            case PENDING_PAYMENT -> switch (event) {
                case PAY_SUCCESS -> PAID;
                case USER_CANCEL -> CANCELLED;
                case PAYMENT_TIMEOUT -> CLOSED;
                default -> throw new IllegalStateException("待支付订单不支持该事件");
            };
            case PAID -> switch (event) {
                case REFUND_REQUESTED -> REFUNDING;
                case FULFILLMENT_FINISHED -> COMPLETED;
                default -> throw new IllegalStateException("已支付订单不支持该事件");
            };
            case REFUNDING -> switch (event) {
                case REFUND_SUCCESS -> REFUNDED;
                case REFUND_FAILED -> PAID;
                default -> throw new IllegalStateException("退款中订单不支持该事件");
            };
            case CANCELLED, CLOSED, COMPLETED, REFUNDED ->
                    throw new IllegalStateException("终态订单不能继续变更");
        };
    }
}
