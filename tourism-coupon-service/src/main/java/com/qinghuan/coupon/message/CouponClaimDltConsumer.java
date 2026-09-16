package com.qinghuan.coupon.message;

import com.qinghuan.coupon.CouponClaimCompensationService;
import com.qinghuan.coupon.CouponClaimConsumerService;
import com.qinghuan.pojo.entity.CouponClaimRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 抢券死信的最小补偿消费者。
 */
@Slf4j
@Component
public class CouponClaimDltConsumer {

    private final CouponClaimConsumerService consumerService;
    private final CouponClaimCompensationService compensationService;

    public CouponClaimDltConsumer(
            CouponClaimConsumerService consumerService,
            CouponClaimCompensationService compensationService) {
        this.consumerService = consumerService;
        this.compensationService = compensationService;
    }

    @KafkaListener(
            topics = CouponKafkaConstant.CLAIM_COMMAND_DLT_TOPIC,
            groupId = CouponKafkaConstant.CLAIM_DLT_CONSUMER_GROUP
    )
    public void consume(CouponClaimCommand command) {
        CouponClaimRequest databaseResult =
                consumerService.findProcessedResult(command.requestId());

        if (databaseResult != null) {
            /*
             * 数据库已经提交，说明异常发生在后续 Redis 同步阶段。
             * 此时只补写结果，不能恢复库存或删除一人一单标记。
             */
            compensationService.syncDatabaseResult(databaseResult);
            log.info(
                    "死信结果已按数据库状态同步，requestId={}，status={}",
                    command.requestId(),
                    databaseResult.getStatus()
            );
            return;
        }

        // MySQL 没有结果，说明数据库事务未提交，可以安全撤销 Redis 预扣。
        compensationService.compensateConsumeFailure(command);
    }
}
