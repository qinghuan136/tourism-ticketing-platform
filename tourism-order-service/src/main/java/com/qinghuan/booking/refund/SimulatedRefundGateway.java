package com.qinghuan.booking.refund;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 本地演示用退款网关。
 * Redis 在这里模拟第三方平台保存的退款结果，应用重启后仍可按 refundNo 查询。
 */
@Component
@ConditionalOnProperty(
        prefix = "app.refund",
        name = "gateway",
        havingValue = "simulated",
        matchIfMissing = true)
public class SimulatedRefundGateway implements RefundGateway {

    private static final String KEY_PREFIX = "simulated:refund:";

    private final StringRedisTemplate redisTemplate;

    public SimulatedRefundGateway(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public RefundGatewayResult requestRefund(RefundRequest request) {
        // 相同 refundNo 反复请求只会得到同一份退款结果。
        redisTemplate.opsForValue().setIfAbsent(
                KEY_PREFIX + request.refundNo(),
                RefundGatewayResult.SUCCESS.name());
        return queryRefund(request.refundNo());
    }

    @Override
    public RefundGatewayResult queryRefund(String refundNo) {
        String result = redisTemplate.opsForValue().get(KEY_PREFIX + refundNo);
        return result == null
                ? RefundGatewayResult.NOT_FOUND
                : RefundGatewayResult.valueOf(result);
    }
}
