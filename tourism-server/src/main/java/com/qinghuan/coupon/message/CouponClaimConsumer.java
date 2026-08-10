package com.qinghuan.coupon.message;

import com.qinghuan.coupon.CouponClaimCompensationService;
import com.qinghuan.coupon.CouponClaimConsumerService;
import com.qinghuan.pojo.entity.CouponClaimRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 优惠券抢券消息消费者。
 */
@Slf4j
@Component
public class CouponClaimConsumer {

    private final CouponClaimConsumerService consumerService;
    private final CouponClaimCompensationService compensationService;

    public CouponClaimConsumer(
            CouponClaimConsumerService consumerService,
            CouponClaimCompensationService compensationService) {
        this.consumerService = consumerService;
        this.compensationService = compensationService;
    }

    @KafkaListener(
            topics = CouponKafkaConstant.CLAIM_COMMAND_TOPIC,
            groupId = CouponKafkaConstant.CLAIM_CONSUMER_GROUP,
            containerFactory = "couponClaimKafkaListenerContainerFactory"
    )
    public void consume(CouponClaimCommand command) {
        log.info(
                "开始处理抢券消息，requestId={}，activityId={}，userId={}",
                command.requestId(),
                command.activityId(),
                command.userId()
        );

        /*
         * process 返回时，MySQL 事务已经提交完成。
         * 此时再更新 Redis，避免数据库回滚后 Redis 却显示 SUCCESS。
         */
        CouponClaimRequest result =
                consumerService.process(command);

        compensationService.syncDatabaseResult(result);

        log.info(
                "抢券消息处理完成，requestId={}，status={}",
                result.getRequestId(),
                result.getStatus()
        );
    }

}
