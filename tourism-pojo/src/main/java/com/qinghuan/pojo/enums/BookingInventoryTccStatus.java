package com.qinghuan.pojo.enums;

/** 订单服务记录的库存 TCC 协调状态，供失败后的定时重试使用。 */
public enum BookingInventoryTccStatus {
    TRYING,
    CONFIRMING,
    CANCELING,
    CONFIRMED,
    CANCELED
}
