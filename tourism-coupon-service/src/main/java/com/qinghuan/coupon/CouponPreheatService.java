package com.qinghuan.coupon;

/**
 * 优惠券活动 Redis 预热服务。
 */
public interface CouponPreheatService {

    /**
     * 扫描并预热当前时间窗口内的活动。
     *
     * @return 本轮成功完成预热的活动数量
     */
    int preheatUpcomingActivities();

    /**
     * 删除指定活动的 Redis 领取数据。
     *
     * 后续活动取消时也会复用这个方法。
     */
    void disableActivityCache(Long activityId);
}