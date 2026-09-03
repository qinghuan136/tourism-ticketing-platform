package com.qinghuan.coupon.message;

import com.qinghuan.coupon.CouponClaimCompensationService;
import com.qinghuan.coupon.CouponClaimConsumerService;
import com.qinghuan.pojo.entity.CouponClaimRequest;
import com.qinghuan.pojo.enums.CouponClaimStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CouponClaimDltConsumerTest {

    private final CouponClaimConsumerService consumerService =
            mock(CouponClaimConsumerService.class);
    private final CouponClaimCompensationService compensationService =
            mock(CouponClaimCompensationService.class);
    private final CouponClaimDltConsumer dltConsumer =
            new CouponClaimDltConsumer(consumerService, compensationService);

    @Test
    void shouldOnlySyncRedisWhenDatabaseAlreadyHasResult() {
        CouponClaimCommand command = command();
        CouponClaimRequest result = new CouponClaimRequest();
        result.setRequestId(command.requestId());
        result.setStatus(CouponClaimStatus.SUCCESS);
        result.setUserCouponId(10L);
        when(consumerService.findProcessedResult(command.requestId()))
                .thenReturn(result);

        dltConsumer.consume(command);

        verify(compensationService).syncDatabaseResult(result);
        verify(compensationService, never()).compensateConsumeFailure(command);
    }

    @Test
    void shouldRestoreRedisWhenDatabaseTransactionDidNotCommit() {
        CouponClaimCommand command = command();
        when(consumerService.findProcessedResult(command.requestId()))
                .thenReturn(null);

        dltConsumer.consume(command);

        verify(compensationService).compensateConsumeFailure(command);
        verify(compensationService, never())
                .syncDatabaseResult(org.mockito.ArgumentMatchers.any());
    }

    private CouponClaimCommand command() {
        return CouponClaimCommand.create(
                "CP-DLT-TEST",
                1L,
                2L,
                LocalDateTime.of(2026, 8, 10, 10, 0)
        );
    }
}
