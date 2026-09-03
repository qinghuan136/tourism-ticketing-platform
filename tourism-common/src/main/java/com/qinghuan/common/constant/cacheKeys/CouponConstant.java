package com.qinghuan.common.constant.cacheKeys;

import java.time.Duration;

/**
 * 优惠券秒杀使用的 Redis Key。
 *
 * 活动相关 Key 使用 {activityId} 作为 Redis Cluster Hash Tag，
 * 保证同一活动的多个 Key 尽量位于同一个槽中。
 */
public final class CouponConstant {

    private static final String ACTIVITY_PREFIX = "coupon:activity:";
    private static final String CLAIM_RESULT_PREFIX = "coupon:claim:";

    /** 活动结束后，Redis 数据额外保留一天，方便处理延迟消息和结果查询。 */
    public static final long ACTIVITY_CACHE_GRACE_SECONDS = 24 * 60 * 60L;

    /** 抢券结果默认保留七天。 */
    public static final long CLAIM_RESULT_TTL_SECONDS = 7 * 24 * 60 * 60L;

    /** 定时补发任务用于发现各活动 Outbox 的非阻塞扫描模式。 */
    public static final String CLAIM_OUTBOX_KEY_PATTERN =
            "coupon:activity:*:claim-outbox";

    /** 活动元数据 Hash 中的字段名。 */
    public static final String META_FIELD_STATUS = "status";
    public static final String META_FIELD_CLAIM_START_AT = "claimStartAt";
    public static final String META_FIELD_CLAIM_END_AT = "claimEndAt";

    private CouponConstant() {
    }

    /**
     * 返回同一活动所有 Redis Key 的公共部分。
     *
     * 示例：coupon:activity:{1001}:
     * 花括号是 Redis Cluster Hash Tag，不是文档占位符。
     */
    private static String activityPrefix(Long activityId) {
        return ACTIVITY_PREFIX + "{" + activityId + "}:";
    }

    /** 活动剩余入口库存。 */
    public static String activityStockKey(Long activityId) {
        return activityPrefix(activityId) + "stock";
    }

    /** 活动是否允许进入抢券逻辑，值使用 1 或 0。 */
    public static String activityEnabledKey(Long activityId) {
        return activityPrefix(activityId) + "enabled";
    }

    /** 活动状态和领取时间等元数据。 */
    public static String activityMetadataKey(Long activityId) {
        return activityPrefix(activityId) + "meta";
    }

    /** 已取得该活动抢券资格的游客集合。 */
    public static String claimedUsersKey(Long activityId) {
        return activityPrefix(activityId) + "claimed-users";
    }

    /** 同一游客在当前活动中对应的 requestId。 */
    public static String userRequestKey(Long activityId, Long userId) {
        return activityPrefix(activityId) + "user-request:" + userId;
    }

    /** 已通过 Lua 取得资格、但尚未确认写入 Kafka 的请求。 */
    public static String claimOutboxKey(Long activityId) {
        return activityPrefix(activityId) + "claim-outbox";
    }

    /** 抢券请求的快速查询结果，例如 PENDING、SUCCESS、FAILED。 */
    public static String claimResultKey(String requestId) {
        return CLAIM_RESULT_PREFIX + "{" + requestId + "}";
    }

    /** 提前多久把即将开始的活动写入 Redis。 */
    public static final Duration window = Duration.ofMinutes(10);

}
