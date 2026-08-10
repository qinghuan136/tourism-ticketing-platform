package com.qinghuan.coupon;

import com.qinghuan.coupon.message.CouponClaimCommand;
import com.qinghuan.pojo.entity.CouponActivity;
import com.qinghuan.pojo.entity.CouponClaimRequest;
import com.qinghuan.pojo.entity.UserCoupon;
import com.qinghuan.pojo.enums.CouponClaimStatus;
import com.qinghuan.pojo.enums.UserCouponStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Kafka 抢券消息的数据库处理服务。
 */
@Service
public class CouponClaimConsumerService {

    private final CouponMapper couponMapper;

    public CouponClaimConsumerService(CouponMapper couponMapper) {
        this.couponMapper = couponMapper;
    }

    /** DLT 补偿前查询 MySQL 最终结果，防止成功发券后误恢复 Redis 库存。 */
    public CouponClaimRequest findProcessedResult(String requestId) {
        return couponMapper.findClaimRequestById(requestId);
    }

    /**
     * 在一个 MySQL 事务中完成库存扣减、发券和结果记录。
     *
     * @return 数据库中的最终抢券结果
     */
    @Transactional
    public CouponClaimRequest process(CouponClaimCommand command) {
        /*
         * Kafka 可能重复投递。
         * requestId 已经存在时说明当前消息处理过，直接返回原结果。
         */
        CouponClaimRequest existing =
                couponMapper.findClaimRequestById(command.requestId());

        if (existing != null) {
            return existing;
        }

        CouponActivity activity =
                couponMapper.findActivityForClaim(command.activityId());

        /*
         * Redis 预扣成功不代表数据库一定能扣减成功。
         * MySQL 是最终库存来源。
         */
        int deducted = couponMapper.decreaseActivityStock(
                command.activityId(),
                command.requestedAt()
        );

        LocalDateTime processedAt = LocalDateTime.now();

        if (deducted == 0) {
            CouponClaimRequest failedRequest =
                    buildFailedRequest(
                            command,
                            processedAt,
                            "SOLD_OUT_OR_UNAVAILABLE"
                    );

            couponMapper.insertClaimRequest(failedRequest);
            return failedRequest;
        }

        /*
         * 把活动规则复制到 user_coupon，
         * 后续活动发生变化也不会影响游客已经领取的券。
         */
        UserCoupon userCoupon = buildUserCoupon(
                activity,
                command.userId(),
                processedAt
        );

        couponMapper.insertUserCoupon(userCoupon);

        CouponClaimRequest successRequest =
                buildSuccessRequest(
                        command,
                        userCoupon.getId(),
                        processedAt
                );

        couponMapper.insertClaimRequest(successRequest);
        return successRequest;
    }

    private UserCoupon buildUserCoupon(
            CouponActivity activity,
            Long userId,
            LocalDateTime acquiredAt) {

        UserCoupon userCoupon = new UserCoupon();

        userCoupon.setActivityId(activity.getId());
        userCoupon.setUserId(userId);
        userCoupon.setVenueId(activity.getVenueId());

        // 保存领取时的活动规则快照。
        userCoupon.setCouponName(activity.getName());
        userCoupon.setThresholdAmount(activity.getThresholdAmount());
        userCoupon.setDiscountAmount(activity.getDiscountAmount());
        userCoupon.setValidFrom(activity.getValidFrom());
        userCoupon.setValidUntil(activity.getValidUntil());

        userCoupon.setStatus(UserCouponStatus.AVAILABLE);
        userCoupon.setAcquiredAt(acquiredAt);

        return userCoupon;
    }

    private CouponClaimRequest buildSuccessRequest(
            CouponClaimCommand command,
            Long userCouponId,
            LocalDateTime processedAt) {

        CouponClaimRequest request = new CouponClaimRequest();

        request.setRequestId(command.requestId());
        request.setActivityId(command.activityId());
        request.setUserId(command.userId());
        request.setStatus(CouponClaimStatus.SUCCESS);
        request.setUserCouponId(userCouponId);
        request.setRequestedAt(command.requestedAt());
        request.setProcessedAt(processedAt);

        return request;
    }

    private CouponClaimRequest buildFailedRequest(
            CouponClaimCommand command,
            LocalDateTime processedAt,
            String failureReason) {

        CouponClaimRequest request = new CouponClaimRequest();

        request.setRequestId(command.requestId());
        request.setActivityId(command.activityId());
        request.setUserId(command.userId());
        request.setStatus(CouponClaimStatus.FAILED);
        request.setFailureReason(failureReason);
        request.setRequestedAt(command.requestedAt());
        request.setProcessedAt(processedAt);

        return request;
    }
}
