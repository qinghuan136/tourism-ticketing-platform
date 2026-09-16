package com.qinghuan.booking;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FeignFallbackFactoryTest {

    @Test
    void venueFallbackShouldRejectInventoryReservation() {
        IllegalStateException cause = new IllegalStateException("connection refused");
        VenueClient fallback = new VenueClientFallbackFactory().create(cause);

        RemoteServiceUnavailableException exception = assertThrows(
                RemoteServiceUnavailableException.class, () -> fallback.tryReserve(null));

        assertEquals("场次库存服务暂时不可用", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void couponFallbackShouldRejectCouponLock() {
        IllegalStateException cause = new IllegalStateException("connection refused");
        CouponClient fallback = new CouponClientFallbackFactory().create(cause);

        RemoteServiceUnavailableException exception = assertThrows(
                RemoteServiceUnavailableException.class, () -> fallback.tryLock(null));

        assertEquals("优惠券服务暂时不可用", exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}
