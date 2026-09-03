package com.qinghuan.pojo.enums;

/**
 * 抢券请求失败原因。
 */
public enum CouponClaimFailureReason {

    /** 消费多次重试后仍未能完成数据库处理。 */
    MESSAGE_CONSUME_FAILED,

    /** MySQL 最终库存不足。 */
    SOLD_OUT,

    /** 活动已经取消、结束或请求时间不合法。 */
    ACTIVITY_UNAVAILABLE
}
