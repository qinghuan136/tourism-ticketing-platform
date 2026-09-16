package com.qinghuan.coupon.message;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * 优惠券抢券消息生产者。
 */
@Slf4j
@Component
public class CouponClaimProducer {

    private final KafkaTemplate<String, CouponClaimCommand> kafkaTemplate;

    public CouponClaimProducer(
            KafkaTemplate<String, CouponClaimCommand> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * 发送抢券命令，并把异步发送结果交给调用方。
     */
    public CompletableFuture<SendResult<String, CouponClaimCommand>> send(
            CouponClaimCommand command) {

        CompletableFuture<SendResult<String, CouponClaimCommand>> future =
                kafkaTemplate.send(
                        CouponKafkaConstant.CLAIM_COMMAND_TOPIC,
                        command.requestId(),
                        command
                );

        /*
         * Producer 这里只记录日志。
         * 具体业务补偿交给 CouponClaimServiceImpl。
         */
        future.whenComplete((result, exception) -> {
            if (exception != null) {
                log.error(
                        "抢券消息发送失败，requestId={}",
                        command.requestId(),
                        exception
                );
                return;
            }

            log.debug(
                    "抢券消息发送成功，requestId={}，partition={}，offset={}",
                    command.requestId(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset()
            );
        });

        return future;
    }
}