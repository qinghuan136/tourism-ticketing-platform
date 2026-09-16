package com.qinghuan.coupon.message;

/**
 * 优惠券异步领取使用的 Kafka 名称。
 */
public final class CouponKafkaConstant {

    /** 抢券命令 Topic。 */
    public static final String CLAIM_COMMAND_TOPIC =
            "coupon-claim-commands-v1";

    /** 多次重试仍失败的消息进入该死信 Topic。 */
    public static final String CLAIM_COMMAND_DLT_TOPIC =
            "coupon-claim-commands-v1-dlt";

    /** 抢券消费者组。 */
    public static final String CLAIM_CONSUMER_GROUP =
            "coupon-claim-consumer-v1";

    /** 死信补偿消费者组。 */
    public static final String CLAIM_DLT_CONSUMER_GROUP =
            "coupon-claim-dlt-compensation-v1";

    private CouponKafkaConstant() {
    }
}
