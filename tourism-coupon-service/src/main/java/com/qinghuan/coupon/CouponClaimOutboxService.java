package com.qinghuan.coupon;

import com.qinghuan.common.constant.cacheKeys.CouponConstant;
import com.qinghuan.coupon.message.CouponClaimCommand;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Redis Outbox 的查询和发送确认操作。 */
@Service
public class CouponClaimOutboxService {

    private final StringRedisTemplate stringRedisTemplate;

    public CouponClaimOutboxService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /** Kafka 已确认接收消息后，删除对应的待发送记录。 */
    public void markSent(CouponClaimCommand command) {
        stringRedisTemplate.opsForZSet().remove(
                CouponConstant.claimOutboxKey(command.activityId()),
                command.toOutboxValue()
        );
    }

    /**
     * 查找超过等待时间仍未确认发送的消息。
     * 使用 SCAN 避免直接执行 KEYS 阻塞 Redis。
     */
    public List<CouponClaimCommand> findPending(
            long createdBeforeEpochMillis,
            int batchSize) {

        List<CouponClaimCommand> commands = new ArrayList<>();
        ScanOptions options = ScanOptions.scanOptions()
                .match(CouponConstant.CLAIM_OUTBOX_KEY_PATTERN)
                .count(100)
                .build();

        try (Cursor<String> keys = stringRedisTemplate.scan(options)) {
            while (keys.hasNext() && commands.size() < batchSize) {
                String outboxKey = keys.next();
                int remaining = batchSize - commands.size();
                Set<String> values = stringRedisTemplate.opsForZSet()
                        .rangeByScore(
                                outboxKey,
                                0,
                                createdBeforeEpochMillis,
                                0,
                                remaining
                        );

                if (values != null) {
                    values.stream()
                            .map(CouponClaimCommand::fromOutboxValue)
                            .forEach(commands::add);
                }
            }
        }

        return commands;
    }
}
