package com.qinghuan.coupon.message;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CouponClaimCommandTest {

    @Test
    void outboxValue_shouldRestoreOriginalCommand() {
        CouponClaimCommand command = CouponClaimCommand.create(
                "CP-OUTBOX-TEST",
                10L,
                20L,
                LocalDateTime.of(2026, 8, 31, 12, 30, 15)
        );

        CouponClaimCommand restored = CouponClaimCommand.fromOutboxValue(
                command.toOutboxValue()
        );

        assertEquals(command, restored);
    }
}
