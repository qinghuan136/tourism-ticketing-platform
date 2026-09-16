package com.qinghuan.booking;

import com.qinghuan.pojo.entity.BookingCouponTccOperation;
import com.qinghuan.pojo.enums.BookingCouponTccStatus;
import com.qinghuan.pojo.remote.coupon.CouponOrderOperationRequest;
import com.qinghuan.pojo.remote.coupon.CouponOrderTryRequest;
import com.qinghuan.pojo.remote.coupon.CouponOrderTryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** 订单侧优惠券 TCC 协调器，二阶段失败由持久化记录驱动重试。 */
@Service
public class BookingCouponTccService {

    private final BookingCouponTccMapper tccMapper;
    private final CouponClient couponClient;

    public BookingCouponTccService(BookingCouponTccMapper tccMapper, CouponClient couponClient) {
        this.tccMapper = tccMapper;
        this.couponClient = couponClient;
    }

    @Transactional
    public void start(Long orderId, Long couponId) {
        tccMapper.insert(orderId, couponId, BookingCouponTccStatus.TRYING);
    }

    public CouponOrderTryResponse tryLock(CouponOrderTryRequest request) {
        CouponOrderTryResponse response = couponClient.tryLock(request).data();
        tccMapper.updateStatus(request.orderId(), BookingCouponTccStatus.TRYING, BookingCouponTccStatus.TRIED);
        return response;
    }

    /** 订单本地事务内标记为二阶段 Confirm，提交后才可以远程确认。 */
    @Transactional
    public void markConfirming(Long orderId) {
        tccMapper.updateStatus(orderId, BookingCouponTccStatus.TRIED, BookingCouponTccStatus.CONFIRMING);
    }

    /** 本地事务回滚后或订单关闭时标记 Cancel；TRYING 覆盖 Try 结果未知的场景。 */
    @Transactional
    public void markCanceling(Long orderId) {
        if (tccMapper.updateStatus(orderId, BookingCouponTccStatus.TRIED, BookingCouponTccStatus.CANCELING) == 0) {
            tccMapper.updateStatus(orderId, BookingCouponTccStatus.TRYING, BookingCouponTccStatus.CANCELING);
        }
    }

    public void confirm(Long orderId, Long couponId, LocalDateTime now) {
        couponClient.confirm(new CouponOrderOperationRequest(orderId, couponId, now));
        tccMapper.updateStatus(orderId, BookingCouponTccStatus.CONFIRMING, BookingCouponTccStatus.CONFIRMED);
    }

    public void cancel(Long orderId, Long couponId, LocalDateTime now) {
        couponClient.cancel(new CouponOrderOperationRequest(orderId, couponId, now));
        tccMapper.updateStatus(orderId, BookingCouponTccStatus.CANCELING, BookingCouponTccStatus.CANCELED);
    }

    public void restoreAfterRefund(Long orderId, Long couponId, LocalDateTime now) {
        couponClient.restoreAfterRefund(new CouponOrderOperationRequest(orderId, couponId, now));
    }

    public List<BookingCouponTccOperation> listPending() {
        return tccMapper.listPending();
    }

    public void retry(BookingCouponTccOperation operation) {
        if (operation.getStatus() == BookingCouponTccStatus.CONFIRMING) {
            confirm(operation.getOrderId(), operation.getCouponId(), LocalDateTime.now());
        } else {
            // TRYING 代表 Try 的结果未知，按 Cancel 执行可安全覆盖空回滚。
            if (operation.getStatus() == BookingCouponTccStatus.TRYING) {
                markCanceling(operation.getOrderId());
            }
            cancel(operation.getOrderId(), operation.getCouponId(), LocalDateTime.now());
        }
    }
}
