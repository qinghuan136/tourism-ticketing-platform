package com.qinghuan.common.constant.cacheKeys;

public class LockConstant {
    /** Redis 缓存未命中时的缓存重建锁前缀。 */
    public static final String LOCK_CACHE_REBUILD_PREFIX =
            "lock:cache:rebuild:";

    // 订单业务相关锁前缀
    public static final String LOCK_BOOKING_PREFIX = "lock:booking:";

    /**
     * 优惠券活动预热锁。
     *
     * 同一活动只允许一个服务实例执行预热，避免多个定时任务
     * 同时覆盖 Redis 数据和重复修改 preheated_at。
     */
    public static final String LOCK_COUPON_PREHEAT_PREFIX =
            "lock:coupon:preheat:";

    /** 退款对账任务的订单级锁，避免多实例重复请求退款平台。 */
    public static final String LOCK_REFUND_RECONCILE_PREFIX =
            "lock:refund:reconcile:";

    /** 库存 TCC 二阶段补偿的订单级锁，避免多实例同时重复调用。 */
    public static final String LOCK_INVENTORY_TCC_PREFIX =
            "lock:inventory:tcc:";
}
