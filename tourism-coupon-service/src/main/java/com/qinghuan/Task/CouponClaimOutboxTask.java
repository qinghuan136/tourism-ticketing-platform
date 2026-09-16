package com.qinghuan.Task;

import com.qinghuan.coupon.CouponClaimOutboxService;
import com.qinghuan.coupon.message.CouponClaimCommand;
import com.qinghuan.coupon.message.CouponClaimProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/** 补发 Redis Outbox 中长时间未获得 Kafka 确认的抢券消息。 */
@Slf4j
@Component
public class CouponClaimOutboxTask {

    private final CouponClaimOutboxService outboxService;
    private final CouponClaimProducer claimProducer;
    private final long pendingMillis;
    private final int batchSize;

    public CouponClaimOutboxTask(
            CouponClaimOutboxService outboxService,
            CouponClaimProducer claimProducer,
            @Value("${app.coupon.claim-outbox.pending-millis:30000}") long pendingMillis,
            @Value("${app.coupon.claim-outbox.batch-size:100}") int batchSize) {
        this.outboxService = outboxService;
        this.claimProducer = claimProducer;
        this.pendingMillis = pendingMillis;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString =
            "${app.coupon.claim-outbox.fixed-delay-millis:10000}")
    public void resendPendingMessages() {
        try {
            long createdBefore = System.currentTimeMillis() - pendingMillis;
            List<CouponClaimCommand> commands =
                    outboxService.findPending(createdBefore, batchSize);

            for (CouponClaimCommand command : commands) {
                claimProducer.send(command)
                        .whenComplete((result, exception) -> {
                            if (exception == null) {
                                outboxService.markSent(command);
                            }
                        });
            }

            if (!commands.isEmpty()) {
                log.info("本轮补发优惠券抢券消息 {} 条", commands.size());
            }
        } catch (Exception exception) {
            // Redis 或 Kafka 暂时不可用时保留 Outbox，下一轮继续重试。
            log.error("优惠券抢券 Outbox 补发失败", exception);
        }
    }
}
