package com.qinghuan.coupon.message;

import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.kafka.autoconfigure.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * 优惠券相关 Kafka Topic 配置。
 */
@Configuration
public class CouponKafkaConfig {

    /**
     * 抢券命令 Topic。
     *
     * 本地只有一个 Kafka Broker，所以副本数设置为 1。
     */
    @Bean
    public NewTopic couponClaimCommandTopic() {
        return TopicBuilder
                .name(CouponKafkaConstant.CLAIM_COMMAND_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    /** 死信 Topic 与主 Topic 保持相同分区数。 */
    @Bean
    public NewTopic couponClaimDltTopic() {
        return TopicBuilder
                .name(CouponKafkaConstant.CLAIM_COMMAND_DLT_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * 主消费者失败后每隔一秒重试两次，仍失败则把原消息投递到 DLT。
     * 该配置只供主抢券消费者使用，避免 DLT 消费失败时再次投回自己。
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<Object, Object>
            couponClaimKafkaListenerContainerFactory(
                    ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
                    ConsumerFactory<Object, Object> consumerFactory,
                    KafkaTemplate<Object, Object> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, consumerFactory);

        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        kafkaTemplate,
                        (record, exception) -> new TopicPartition(
                                CouponKafkaConstant.CLAIM_COMMAND_DLT_TOPIC,
                                record.partition()
                        )
                );
        factory.setCommonErrorHandler(
                new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2L))
        );
        return factory;
    }
}
