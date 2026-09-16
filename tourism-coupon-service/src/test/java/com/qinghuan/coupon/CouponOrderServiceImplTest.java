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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponOrderServiceImplTest {

    @Mock
    private CouponMapper couponMapper;

    private CouponOrderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CouponOrderServiceImpl(couponMapper);
    }

    @Test
    void shouldLockApplicableCouponAndReturnDiscountSnapshot() {
        UserCoupon coupon = coupon(UserCouponStatus.AVAILABLE);
        when(couponMapper.findCouponForOrder(2001L, 7L)).thenReturn(coupon);
        when(couponMapper.lockCouponForOrder(anyLong(), anyLong(), anyLong(), anyLong(), any())).thenReturn(1);

        CouponOrderTryResponse result = service.tryLock(request());

        assertEquals(new BigDecimal("20.00"), result.discountAmount());
        assertEquals(new BigDecimal("100.00"), result.payableAmount());
        verify(couponMapper).insertOrderReservation(any());
    }

    @Test
    void shouldRejectOrderBelowCouponThreshold() {
        when(couponMapper.findCouponForOrder(2001L, 7L)).thenReturn(coupon(UserCouponStatus.AVAILABLE));

        CouponOrderTryRequest request = new CouponOrderTryRequest(501L, 2001L, 7L, 10L,
                new BigDecimal("99.99"));
        BusinessException exception = assertThrows(BusinessException.class, () -> service.tryLock(request));

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        verify(couponMapper, never()).lockCouponForOrder(anyLong(), anyLong(), anyLong(), anyLong(), any());
    }

    @Test
    void shouldMakeRepeatedConfirmIdempotent() {
        CouponOrderReservation reservation = new CouponOrderReservation();
        reservation.setCouponId(2001L);
        reservation.setStatus(CouponOrderReservationStatus.CONFIRMED);
        when(couponMapper.findOrderReservation(501L)).thenReturn(reservation);

        service.confirm(new CouponOrderOperationRequest(501L, 2001L, LocalDateTime.now()));

        verify(couponMapper, never()).markCouponUsed(anyLong(), anyLong(), any());
    }

    @Test
    void shouldReturnSameResultWhenConcurrentTryAlreadyCreatedReservation() {
        CouponOrderReservation reservation = new CouponOrderReservation();
        reservation.setCouponId(2001L);
        reservation.setStatus(CouponOrderReservationStatus.RESERVED);
        when(couponMapper.findCouponForOrder(2001L, 7L)).thenReturn(coupon(UserCouponStatus.AVAILABLE));
        when(couponMapper.lockCouponForOrder(anyLong(), anyLong(), anyLong(), anyLong(), any())).thenReturn(0);
        when(couponMapper.findOrderReservation(501L)).thenReturn(null, reservation);
        when(couponMapper.findCouponById(2001L)).thenReturn(coupon(UserCouponStatus.LOCKED));

        CouponOrderTryResponse result = service.tryLock(request());

        assertEquals(new BigDecimal("100.00"), result.payableAmount());
    }

    @Test
    void shouldRecordEmptyCancelToPreventLateTry() {
        when(couponMapper.findOrderReservation(501L)).thenReturn(null);

        service.cancel(new CouponOrderOperationRequest(501L, 2001L, LocalDateTime.now()));

        verify(couponMapper).insertOrderReservation(any());
    }

    private CouponOrderTryRequest request() {
        return new CouponOrderTryRequest(501L, 2001L, 7L, 10L, new BigDecimal("120.00"));
    }

    private UserCoupon coupon(UserCouponStatus status) {
        UserCoupon coupon = new UserCoupon();
        coupon.setId(2001L);
        coupon.setUserId(7L);
        coupon.setVenueId(10L);
        coupon.setThresholdAmount(new BigDecimal("100.00"));
        coupon.setDiscountAmount(new BigDecimal("20.00"));
        coupon.setStatus(status);
        return coupon;
    }
}
