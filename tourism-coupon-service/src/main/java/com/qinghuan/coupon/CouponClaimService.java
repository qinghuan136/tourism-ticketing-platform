package com.qinghuan.coupon;

import com.qinghuan.pojo.vo.CouponClaimAcceptedVO;

/**
 * 优惠券抢券入口。
 */
public interface CouponClaimService {

    /**
     * 使用 Redis Lua 尝试取得当前活动的抢券资格。
     */
    CouponClaimAcceptedVO claim(Long activityId);
}