package com.qinghuan.coupon;

import com.qinghuan.auth.context.UserContext;
import com.qinghuan.common.constant.cacheKeys.CouponConstant;
import com.qinghuan.common.exception.BusinessException;
import com.qinghuan.common.exception.ErrorCode;
import com.qinghuan.coupon.message.CouponClaimCommand;
import com.qinghuan.coupon.message.CouponClaimProducer;
import com.qinghuan.pojo.enums.CouponClaimStatus;
import com.qinghuan.pojo.vo.CouponClaimAcceptedVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class CouponClaimServiceImpl implements CouponClaimService {

    private static final int ACCEPTED = 0;
    private static final int DUPLICATE = 1;
    private static final int CACHE_NOT_READY = 2;
    private static final int NOT_STARTED = 3;
    private static final int ENDED = 4;
    private static final int SOLD_OUT = 5;
    private final StringRedisTemplate stringRedisTemplate;
    private final CouponClaimProducer claimProducer;
    private final CouponClaimOutboxService outboxService;

    /**
     * Lua 脚本只加载一次，后续每次请求直接复用。
     */
    private static final DefaultRedisScript<Long> CLAIM_SCRIPT =
            new DefaultRedisScript<>();

    static {
        CLAIM_SCRIPT.setLocation(
                new ClassPathResource("scripts/coupon_claim.lua")
        );
        CLAIM_SCRIPT.setResultType(Long.class);
    }

    public CouponClaimServiceImpl(
            StringRedisTemplate stringRedisTemplate,
            CouponClaimProducer claimProducer,
            CouponClaimOutboxService outboxService) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.claimProducer = claimProducer;
        this.outboxService = outboxService;
    }

    @Override
    public CouponClaimAcceptedVO claim(Long activityId) {
        Long userId = UserContext.getUserId();
        LocalDateTime requestedAt = LocalDateTime.now();

        /*
         * requestId 在执行 Lua 前生成。
         * 如果属于重复请求，Lua 会忽略这个新值并返回原 requestId。
         */
        String requestId = generateRequestId();
        CouponClaimCommand command = CouponClaimCommand.create(
                requestId,
                activityId,
                userId,
                requestedAt
        );
        String userRequestKey =
                CouponConstant.userRequestKey(activityId, userId);

        Long result = stringRedisTemplate.execute(
                CLAIM_SCRIPT,
                List.of(
                        CouponConstant.activityEnabledKey(activityId),
                        CouponConstant.activityMetadataKey(activityId),
                        CouponConstant.activityStockKey(activityId),
                        CouponConstant.claimedUsersKey(activityId),
                        userRequestKey,
                        CouponConstant.claimOutboxKey(activityId)
                ),
                userId.toString(),
                requestId,
                String.valueOf(System.currentTimeMillis()),
                command.toOutboxValue()
        );

        return switch (result.intValue()) {
            case ACCEPTED -> {
                /*
                 * 保存请求结果，用于查询结果。
                 */
                savePendingResult(
                        requestId,
                        activityId,
                        userId
                );

                /*
                 * 只有第一次取得 Redis 资格的请求才发送 Kafka 消息。
                 * 重复请求只返回原 requestId，不重复投递。
                 */
                sendClaimCommand(command);

                yield accepted(requestId);
            }

            case DUPLICATE -> {
                /*
                 * Lua 已确认该游客提交过，这里读取第一次生成的请求号。
                 * 重复点击不会产生新的抢券资格。
                 */
                String existingRequestId =
                        stringRedisTemplate.opsForValue()
                                .get(userRequestKey);

                yield accepted(existingRequestId);
            }

            case CACHE_NOT_READY ->
                    throw new BusinessException(
                            ErrorCode.CONFLICT,
                            "优惠券活动尚未准备完成"
                    );

            case NOT_STARTED ->
                    throw new BusinessException(
                            ErrorCode.CONFLICT,
                            "优惠券活动尚未开始"
                    );

            case ENDED ->
                    throw new BusinessException(
                            ErrorCode.CONFLICT,
                            "优惠券活动已结束或已取消"
                    );

            case SOLD_OUT ->
                    throw new BusinessException(
                            ErrorCode.CONFLICT,
                            "优惠券已抢完"
                    );

            default ->
                    throw new BusinessException(
                            ErrorCode.INTERNAL_ERROR,
                            "抢券脚本返回未知结果"
                    );
        };
    }

    /**
     * 记录抢券请求的临时处理状态。
     *
     * Consumer 完成数据库处理后，再把状态更新为 SUCCESS 或 FAILED。
     */
    private void savePendingResult(
            String requestId,
            Long activityId,
            Long userId) {

        String resultKey =
                CouponConstant.claimResultKey(requestId);

        /*
         * 保存 userId 是为了查询结果时校验请求属于当前游客，
         * 不能只通过 requestId 返回结果。
         */
        stringRedisTemplate.opsForHash().putAll(
                resultKey,
                Map.of(
                        "requestId", requestId,
                        "activityId", activityId.toString(),
                        "userId", userId.toString(),
                        "status", CouponClaimStatus.PENDING.name()
                )
        );

        stringRedisTemplate.expire(
                resultKey,
                CouponConstant.CLAIM_RESULT_TTL_SECONDS,
                TimeUnit.SECONDS
        );
    }

    private CouponClaimAcceptedVO accepted(String requestId) {
        return new CouponClaimAcceptedVO(
                requestId,
                CouponClaimStatus.PENDING
        );
    }

    /** 首次尽快发送；失败或进程宕机时由 Redis Outbox 定时补发。 */
    private void sendClaimCommand(CouponClaimCommand command) {
        try {
            claimProducer.send(command)
                    .whenComplete((sendResult, exception) -> {
                        if (exception == null) {
                            // Broker 确认后才能删除 Outbox。
                            outboxService.markSent(command);
                        }
                    });
        } catch (RuntimeException exception) {
            // Outbox 已由 Lua 写入，当前请求仍可返回 PENDING。
            log.warn("抢券消息首次发送失败，等待 Outbox 补发，requestId={}",
                    command.requestId(), exception);
        }
    }

    /**
     * UUID 去掉分隔符后长度为 32，
     * 加上 CP 前缀后仍小于数据库 request_id 的 64 字符限制。
     */
    private String generateRequestId() {
        return "CP" + UUID.randomUUID()
                .toString()
                .replace("-", "");
    }
}
