package com.qinghuan.coupon.message;

import java.time.LocalDateTime;

/**
 * 游客取得 Redis 抢券资格后发送到 Kafka 的命令消息。
 *
 * 消费者根据 requestId 保证消息幂等，
 * 根据 activityId + userId 保证同一活动一人一券。
 */
public record CouponClaimCommand(
        String requestId,
        Long activityId,
        Long userId,
        LocalDateTime requestedAt,
        int schemaVersion) {

    private static final String OUTBOX_SEPARATOR = "|";

    /** 当前消息结构版本。 */
    public static final int CURRENT_SCHEMA_VERSION = 1;

    /**
     * 创建当前版本的抢券命令，避免调用方到处手写版本号。
     */
    public static CouponClaimCommand create(
            String requestId,
            Long activityId,
            Long userId,
            LocalDateTime requestedAt) {
        return new CouponClaimCommand(
                requestId,
                activityId,
                userId,
                requestedAt,
                CURRENT_SCHEMA_VERSION
        );
    }

    /** 转换成 Redis ZSet member，供宕机后的补发任务重建消息。 */
    public String toOutboxValue() {
        return String.join(
                OUTBOX_SEPARATOR,
                requestId,
                activityId.toString(),
                userId.toString(),
                requestedAt.toString(),
                String.valueOf(schemaVersion)
        );
    }

    /** 从 Redis Outbox 记录恢复 Kafka 消息。 */
    public static CouponClaimCommand fromOutboxValue(String value) {
        String[] parts = value.split("\\|", -1);
        return new CouponClaimCommand(
                parts[0],
                Long.valueOf(parts[1]),
                Long.valueOf(parts[2]),
                LocalDateTime.parse(parts[3]),
                Integer.parseInt(parts[4])
        );
    }
}
