package com.qinghuan.coupon;

import java.time.LocalDateTime;
import com.qinghuan.pojo.remote.coupon.CouponOrderOperationRequest;
import com.qinghuan.pojo.remote.coupon.CouponOrderTryRequest;
import com.qinghuan.pojo.remote.coupon.CouponOrderTryResponse;

/** 优惠券模块提供给订单模块使用的同步事务能力。 */
public interface CouponOrderService {
    /**
     * 校验券归属、景点、状态、有效期和门槛后原子锁券，并返回订单计价结果。
     */
    CouponOrderTryResponse tryLock(CouponOrderTryRequest request);

    /** 支付成功或零元订单创建成功时，将 LOCKED 券转为 USED。 */
    void confirm(CouponOrderOperationRequest request);

    /** 待支付订单取消或超时关闭时释放 LOCKED 券。 */
    void cancel(CouponOrderOperationRequest request);

    /** 已支付订单整单退款时恢复 USED 券。 */
    void restoreAfterRefund(CouponOrderOperationRequest request);

    /** 清理长期未完成二阶段的锁券。 */
    int releaseExpiredLocks(LocalDateTime lockedBefore, LocalDateTime now);
}
