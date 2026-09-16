package com.qinghuan.coupon.model;

import com.qinghuan.common.constant.cacheKeys.CouponConstant;
import com.qinghuan.pojo.enums.CouponActivityStatus;

import java.util.Map;

/**
 * Redis 中保存的优惠券活动元数据。
 *
 * 时间使用毫秒时间戳，便于 Lua 直接进行数字比较，
 * 避免在脚本中解析 LocalDateTime 字符串。
 */
public record CouponActivityCacheMetadata(
        Long activityId,
        CouponActivityStatus status,
        long claimStartAtEpochMilli,
        long claimEndAtEpochMilli) {

    /**
     * 转换成 Redis Hash 可以直接写入的字符串结构。
     */
    public Map<String, String> toHash() {
        return Map.of(
                CouponConstant.META_FIELD_STATUS, status.name(),
                CouponConstant.META_FIELD_CLAIM_START_AT,
                String.valueOf(claimStartAtEpochMilli),
                CouponConstant.META_FIELD_CLAIM_END_AT,
                String.valueOf(claimEndAtEpochMilli)
        );
    }
}