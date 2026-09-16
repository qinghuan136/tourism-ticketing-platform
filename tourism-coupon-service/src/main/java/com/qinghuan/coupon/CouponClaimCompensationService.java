package com.qinghuan.coupon;

import com.qinghuan.common.constant.cacheKeys.CouponConstant;
import com.qinghuan.coupon.message.CouponClaimCommand;
import com.qinghuan.pojo.entity.CouponClaimRequest;
import com.qinghuan.pojo.enums.CouponClaimFailureReason;
import com.qinghuan.pojo.enums.CouponClaimStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 抢券结果与失败补偿的 Redis 修正服务。
 */
@Slf4j
@Service
public class CouponClaimCompensationService {

    private static final DefaultRedisScript<Long> SEND_FAILURE_SCRIPT =
            new DefaultRedisScript<>();

    static {
        SEND_FAILURE_SCRIPT.setLocation(
                new ClassPathResource(
                        "scripts/coupon_claim_compensate.lua"
                )
        );
        SEND_FAILURE_SCRIPT.setResultType(Long.class);
    }

    private final StringRedisTemplate stringRedisTemplate;

    public CouponClaimCompensationService(
            StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /** 消息进入 DLT 且 MySQL 没有结果时，撤销 Redis 预扣。 */
    public void compensateConsumeFailure(CouponClaimCommand command) {
        compensate(
                command,
                CouponClaimFailureReason.MESSAGE_CONSUME_FAILED
        );
    }

    private void compensate(
            CouponClaimCommand command,
            CouponClaimFailureReason failureReason) {

        Long compensated = stringRedisTemplate.execute(
                SEND_FAILURE_SCRIPT,
                List.of(
                        CouponConstant.userRequestKey(
                                command.activityId(),
                                command.userId()
                        ),
                        CouponConstant.activityStockKey(
                                command.activityId()
                        ),
                        CouponConstant.claimedUsersKey(
                                command.activityId()
                        ),
                        CouponConstant.claimOutboxKey(
                                command.activityId()
                        )
                ),
                command.requestId(),
                command.userId().toString(),
                command.toOutboxValue()
        );

        /*
         * 请求已经不能进入数据库处理，
         * 所以无论补偿是否为首次执行，都把当前请求标记为失败。
         */
        markRequestFailed(
                command,
                failureReason
        );

        log.warn(
                "抢券失败补偿完成，requestId={}，reason={}，compensated={}",
                command.requestId(),
                failureReason,
                compensated
        );
    }

    /** MySQL 已有最终结果时只同步 Redis，绝不能恢复已扣减的库存。 */
    public void syncDatabaseResult(CouponClaimRequest result) {
        String resultKey = CouponConstant.claimResultKey(result.getRequestId());

        stringRedisTemplate.opsForHash().putAll(
                resultKey,
                Map.of(
                        "requestId", result.getRequestId(),
                        "activityId", result.getActivityId().toString(),
                        "userId", result.getUserId().toString(),
                        "status", result.getStatus().name()
                )
        );

        if (result.getStatus() == CouponClaimStatus.SUCCESS) {
            stringRedisTemplate.opsForHash().put(
                    resultKey,
                    "userCouponId",
                    result.getUserCouponId().toString()
            );
        }

        if (result.getStatus() == CouponClaimStatus.FAILED) {
            stringRedisTemplate.opsForHash().put(
                    resultKey,
                    "failureReason",
                    result.getFailureReason()
            );
        }

        stringRedisTemplate.expire(
                resultKey,
                CouponConstant.CLAIM_RESULT_TTL_SECONDS,
                TimeUnit.SECONDS
        );
    }

    private void markRequestFailed(
            CouponClaimCommand command,
            CouponClaimFailureReason failureReason) {

        String resultKey =
                CouponConstant.claimResultKey(command.requestId());

        stringRedisTemplate.opsForHash().putAll(
                resultKey,
                Map.of(
                        "requestId", command.requestId(),
                        "activityId", command.activityId().toString(),
                        "userId", command.userId().toString(),
                        "status",
                        CouponClaimStatus.FAILED.name(),
                        "failureReason",
                        failureReason.name()
                )
        );

        stringRedisTemplate.expire(
                resultKey,
                CouponConstant.CLAIM_RESULT_TTL_SECONDS,
                TimeUnit.SECONDS
        );
    }
}
