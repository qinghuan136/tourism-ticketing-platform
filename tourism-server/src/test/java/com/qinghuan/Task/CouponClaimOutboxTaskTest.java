package com.qinghuan.Task;

import com.qinghuan.coupon.CouponClaimOutboxService;
import com.qinghuan.coupon.message.CouponClaimCommand;
import com.qinghuan.coupon.message.CouponClaimProducer;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.SendResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CouponClaimOutboxTaskTest {

    private final CouponClaimOutboxService outboxService =
            mock(CouponClaimOutboxService.class);
    private final CouponClaimProducer claimProducer =
            mock(CouponClaimProducer.class);
    private final CouponClaimOutboxTask task =
            new CouponClaimOutboxTask(outboxService, claimProducer, 30_000L, 100);

    @Test
    void resendPendingMessages_shouldRemoveOutboxAfterKafkaAcknowledgement() {
        CouponClaimCommand command = command();
        when(outboxService.findPending(anyLong(), eq(100)))
                .thenReturn(List.of(command));
        when(claimProducer.send(command))
                .thenReturn(CompletableFuture.completedFuture(null));

        task.resendPendingMessages();

        verify(outboxService).markSent(command);
    }

    @Test
    void resendPendingMessages_shouldKeepOutboxWhenKafkaSendFails() {
        CouponClaimCommand command = command();
        CompletableFuture<SendResult<String, CouponClaimCommand>> failed =
                CompletableFuture.failedFuture(new RuntimeException("Kafka unavailable"));
        when(outboxService.findPending(anyLong(), eq(100)))
                .thenReturn(List.of(command));
        when(claimProducer.send(command)).thenReturn(failed);

        task.resendPendingMessages();

        verify(outboxService, never()).markSent(command);
    }

    private CouponClaimCommand command() {
        return CouponClaimCommand.create(
                "CP-OUTBOX-TEST",
                1L,
                2L,
                LocalDateTime.of(2026, 8, 31, 12, 0)
        );
    }
}
