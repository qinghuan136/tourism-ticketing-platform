package com.qinghuan.coupon;

import com.qinghuan.common.exception.BusinessException;
import com.qinghuan.common.exception.ErrorCode;
import com.qinghuan.pojo.entity.UserCoupon;
import com.qinghuan.pojo.enums.UserCouponStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
    void shouldLockApplicableCouponAndCalculatePayableAmount() {
        UserCoupon coupon = coupon(UserCouponStatus.AVAILABLE);
        when(couponMapper.findCouponForOrder(2001L, 7L)).thenReturn(coupon);
        when(couponMapper.lockCouponForOrder(any(), any(), any(), any())).thenReturn(1);

        CouponDiscount result = service.lockForOrder(
                2001L, 7L, 10L, new BigDecimal("120.00"));

        assertEquals(new BigDecimal("20.00"), result.discountAmount());
        assertEquals(new BigDecimal("100.00"), result.payableAmount());
        verify(couponMapper).lockCouponForOrder(any(), any(), any(), any());
    }

    @Test
    void shouldRejectOrderBelowCouponThreshold() {
        when(couponMapper.findCouponForOrder(2001L, 7L))
                .thenReturn(coupon(UserCouponStatus.AVAILABLE));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.lockForOrder(
                        2001L, 7L, 10L, new BigDecimal("99.99")));

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        verify(couponMapper, never()).lockCouponForOrder(any(), any(), any(), any());
    }

    @Test
    void shouldRejectWhenConditionalLockLosesRace() {
        when(couponMapper.findCouponForOrder(2001L, 7L))
                .thenReturn(coupon(UserCouponStatus.AVAILABLE));
        when(couponMapper.lockCouponForOrder(any(), any(), any(), any())).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.lockForOrder(
                        2001L, 7L, 10L, new BigDecimal("120.00")));

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
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
