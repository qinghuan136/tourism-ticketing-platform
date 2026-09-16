package com.qinghuan.coupon;

import com.qinghuan.common.exception.BusinessException;
import com.qinghuan.common.exception.ErrorCode;
import com.qinghuan.pojo.entity.CouponOrderReservation;
import com.qinghuan.pojo.entity.UserCoupon;
import com.qinghuan.pojo.enums.CouponOrderReservationStatus;
import com.qinghuan.pojo.enums.UserCouponStatus;
import com.qinghuan.pojo.remote.coupon.CouponOrderOperationRequest;
import com.qinghuan.pojo.remote.coupon.CouponOrderTryRequest;
import com.qinghuan.pojo.remote.coupon.CouponOrderTryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 优惠券服务内的轻量 TCC：订单号是三阶段操作的唯一业务键。 */
@Service
public class CouponOrderServiceImpl implements CouponOrderService {

    private final CouponMapper couponMapper;

    public CouponOrderServiceImpl(CouponMapper couponMapper) {
        this.couponMapper = couponMapper;
    }

    @Override
    @Transactional
    public CouponOrderTryResponse tryLock(CouponOrderTryRequest request) {
        CouponOrderReservation reservation = couponMapper.findOrderReservation(request.orderId());
        if (reservation != null) {
            if (!reservation.getCouponId().equals(request.couponId())) {
                throw new BusinessException(ErrorCode.CONFLICT, "订单优惠券预留不一致");
            }
            if (reservation.getStatus() == CouponOrderReservationStatus.CANCELED) {
                // Cancel 先到时留下的围栏，防止延迟 Try 再次锁券。
                throw new BusinessException(ErrorCode.CONFLICT, "优惠券预留已取消");
            }
            return toTryResponse(requireCoupon(request.couponId()), request.originalAmount());
        }

        UserCoupon coupon = couponMapper.findCouponForOrder(request.couponId(), request.userId());
        if (coupon == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "优惠券不存在");
        }
        if (!coupon.getVenueId().equals(request.venueId())) {
            throw new BusinessException(ErrorCode.CONFLICT, "优惠券不适用于当前景点");
        }
        if (coupon.getStatus() != UserCouponStatus.AVAILABLE) {
            throw new BusinessException(ErrorCode.CONFLICT, "优惠券当前不可用");
        }
        if (request.originalAmount().compareTo(coupon.getThresholdAmount()) < 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "订单金额未达到优惠券使用门槛");
        }

        LocalDateTime now = LocalDateTime.now();
        if (couponMapper.lockCouponForOrder(request.couponId(), request.userId(), request.venueId(),
                request.orderId(), now) == 0) {
            // 两个相同 Try 并发到达时，后一个条件更新会落空；再按 orderId 读取即可返回同一结果。
            CouponOrderReservation concurrentReservation = couponMapper.findOrderReservation(request.orderId());
            if (concurrentReservation != null
                    && concurrentReservation.getCouponId().equals(request.couponId())
                    && concurrentReservation.getStatus() != CouponOrderReservationStatus.CANCELED) {
                return toTryResponse(requireCoupon(request.couponId()), request.originalAmount());
            }
            throw new BusinessException(ErrorCode.CONFLICT, "优惠券状态或有效期已发生变化");
        }

        CouponOrderReservation created = new CouponOrderReservation();
        created.setOrderId(request.orderId());
        created.setCouponId(request.couponId());
        created.setStatus(CouponOrderReservationStatus.RESERVED);
        couponMapper.insertOrderReservation(created);
        return toTryResponse(coupon, request.originalAmount());
    }

    @Override
    @Transactional
    public void confirm(CouponOrderOperationRequest request) {
        CouponOrderReservation reservation = couponMapper.findOrderReservation(request.orderId());
        if (reservation == null || !reservation.getCouponId().equals(request.couponId())) {
            throw new BusinessException(ErrorCode.CONFLICT, "优惠券预留不存在");
        }
        if (reservation.getStatus() == CouponOrderReservationStatus.CONFIRMED) {
            return;
        }
        if (reservation.getStatus() == CouponOrderReservationStatus.CANCELED) {
            throw new BusinessException(ErrorCode.CONFLICT, "优惠券预留已取消");
        }

        if (couponMapper.markCouponUsed(request.couponId(), request.orderId(), request.operatedAt()) == 0) {
            UserCoupon coupon = requireCoupon(request.couponId());
            if (!(coupon.getStatus() == UserCouponStatus.USED
                    && request.orderId().equals(coupon.getUsedOrderId()))) {
                throw new BusinessException(ErrorCode.CONFLICT, "优惠券状态发生变化");
            }
        }
        couponMapper.updateOrderReservationStatus(request.orderId(), CouponOrderReservationStatus.RESERVED,
                CouponOrderReservationStatus.CONFIRMED);
    }

    @Override
    @Transactional
    public void cancel(CouponOrderOperationRequest request) {
        CouponOrderReservation reservation = couponMapper.findOrderReservation(request.orderId());
        if (reservation == null) {
            // 空回滚也需要落记录，阻止后到的 Try 造成悬挂。
            CouponOrderReservation canceled = new CouponOrderReservation();
            canceled.setOrderId(request.orderId());
            canceled.setCouponId(request.couponId());
            canceled.setStatus(CouponOrderReservationStatus.CANCELED);
            couponMapper.insertOrderReservation(canceled);
            return;
        }
        if (!reservation.getCouponId().equals(request.couponId())
                || reservation.getStatus() != CouponOrderReservationStatus.RESERVED) {
            return;
        }

        // 条件更新只会释放当前 orderId 自己锁住的券，重复 Cancel 不会重复恢复。
        couponMapper.releaseLockedCoupon(request.couponId(), request.orderId(), request.operatedAt());
        couponMapper.updateOrderReservationStatus(request.orderId(), CouponOrderReservationStatus.RESERVED,
                CouponOrderReservationStatus.CANCELED);
    }

    @Override
    @Transactional
    public void restoreAfterRefund(CouponOrderOperationRequest request) {
        // 已恢复或券随后被其他订单使用时都无需再改动，保持请求幂等。
        couponMapper.restoreUsedCoupon(request.couponId(), request.orderId(), request.operatedAt());
    }

    @Override
    public int releaseExpiredLocks(LocalDateTime lockedBefore, LocalDateTime now) {
        return couponMapper.releaseExpiredLockedCoupons(lockedBefore, now);
    }

    private UserCoupon requireCoupon(Long couponId) {
        UserCoupon coupon = couponMapper.findCouponById(couponId);
        if (coupon == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "优惠券不存在");
        }
        return coupon;
    }

    private CouponOrderTryResponse toTryResponse(UserCoupon coupon, BigDecimal originalAmount) {
        return new CouponOrderTryResponse(coupon.getId(), coupon.getDiscountAmount(),
                originalAmount.subtract(coupon.getDiscountAmount()));
    }
}
