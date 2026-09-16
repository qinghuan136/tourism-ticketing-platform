package com.qinghuan.coupon;

import com.qinghuan.pojo.dto.CouponActivityPageQueryDTO;
import com.qinghuan.pojo.dto.UserCouponQueryDTO;
import com.qinghuan.pojo.entity.CouponActivity;
import com.qinghuan.pojo.entity.CouponClaimRequest;
import com.qinghuan.pojo.entity.CouponOrderReservation;
import com.qinghuan.pojo.entity.UserCoupon;
import com.qinghuan.pojo.enums.CouponActivityStatus;
import com.qinghuan.pojo.enums.CouponOrderReservationStatus;
import com.qinghuan.pojo.vo.CatalogCouponActivityVO;
import com.qinghuan.pojo.vo.CouponActivityVO;
import com.qinghuan.pojo.vo.CouponClaimResultVO;
import com.qinghuan.pojo.vo.UserCouponVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CouponMapper {

    /** 查询当前景点的活动，交给 PageHelper 分页。 */
    List<CouponActivityVO> listActivities(@Param("venueId") Long venueId,
                                          @Param("query") CouponActivityPageQueryDTO query);

    /** 活动详情始终携带景点范围，避免运营者跨景点访问。 */
    CouponActivityVO findActivity(@Param("activityId") Long activityId,
                                  @Param("venueId") Long venueId);

    /** 新建草稿并回填活动主键。 */
    int insertActivity(CouponActivity activity);

    /** 只有数据库中仍为草稿时才允许覆盖活动规则。 */
    int updateDraftActivity(CouponActivity activity);

    /** 携带原状态更新活动，防止发布、取消等并发操作互相覆盖。 */
    int updateActivityStatus(@Param("activityId") Long activityId,
                             @Param("venueId") Long venueId,
                             @Param("oldStatus") CouponActivityStatus oldStatus,
                             @Param("newStatus") CouponActivityStatus newStatus);

    /** 将领取期限已经结束的已发布活动转为结束状态。 */
    int endExpiredActivities(LocalDateTime now);

    /** 查询公开目录中仍未超过领取结束时间的活动。 */
    List<CatalogCouponActivityVO> listCatalogActivities(@Param("venueId") Long venueId,
                                                        @Param("now") LocalDateTime now);

    /** 查询当前游客自己的抢券最终结果。 */
    CouponClaimResultVO findClaimResult(@Param("requestId") String requestId,
                                        @Param("userId") Long userId);

    /** 查询当前游客持有的优惠券。 */
    List<UserCouponVO> listUserCoupons(@Param("userId") Long userId,
                                       @Param("query") UserCouponQueryDTO query);

    /** 定时将到期且未锁定的优惠券改为已过期。 */
    int expireAvailableCoupons(LocalDateTime now);

    /** 订单使用前读取优惠券快照，用于校验门槛和景点。 */
    UserCoupon findCouponForOrder(@Param("couponId") Long couponId,
                                  @Param("userId") Long userId);

    /** 原子锁定当前仍可用且处于有效期内的优惠券。 */
    int lockCouponForOrder(@Param("couponId") Long couponId,
                           @Param("userId") Long userId,
                           @Param("venueId") Long venueId,
                           @Param("orderId") Long orderId,
                           @Param("now") LocalDateTime now);

    /** 支付成功后把订单锁定的优惠券正式核销。 */
    int markCouponUsed(@Param("couponId") Long couponId,
                       @Param("orderId") Long orderId,
                       @Param("now") LocalDateTime now);

    /** 取消或超时后释放锁定券；到期券直接进入 EXPIRED。 */
    int releaseLockedCoupon(@Param("couponId") Long couponId,
                            @Param("orderId") Long orderId,
                            @Param("now") LocalDateTime now);

    /** 整单退款后恢复已使用券；到期券直接进入 EXPIRED。 */
    int restoreUsedCoupon(@Param("couponId") Long couponId,
                          @Param("orderId") Long orderId,
                          @Param("now") LocalDateTime now);

    UserCoupon findCouponById(@Param("couponId") Long couponId);

    CouponOrderReservation findOrderReservation(@Param("orderId") Long orderId);

    int insertOrderReservation(CouponOrderReservation reservation);

    int updateOrderReservationStatus(@Param("orderId") Long orderId,
                                     @Param("oldStatus") CouponOrderReservationStatus oldStatus,
                                     @Param("newStatus") CouponOrderReservationStatus newStatus);

    int releaseExpiredLockedCoupons(@Param("lockedBefore") LocalDateTime lockedBefore,
                                    @Param("now") LocalDateTime now);

    /**
     * 查询即将开始或者已经开始但仍未完成预热的活动。
     *
     * preheatDeadline 表示本轮预热窗口的结束时间，
     */
    List<CouponActivity> listActivitiesToPreheat(
            @Param("now") LocalDateTime now,
            @Param("preheatDeadline") LocalDateTime preheatDeadline
    );

    /**
     * Redis 数据完整写入后，将活动标记为已预热。
     *
     * SQL 仍然校验活动状态和领取结束时间，
     * 防止预热期间活动被运营者取消。
     */
    int markActivityCacheReady(
            @Param("activityId") Long activityId,
            @Param("preheatedAt") LocalDateTime preheatedAt);

    /** Redis 入口启用失败时撤销预热标记，使后续定时任务能够重新预热。 */
    int resetActivityCacheReady(@Param("activityId") Long activityId);

    /**
     * 按 requestId 查询数据库处理结果，用于消费幂等。
     */
    CouponClaimRequest findClaimRequestById(
            @Param("requestId") String requestId);

    /**
     * 查询活动规则，用于生成游客优惠券快照。
     */
    CouponActivity findActivityForClaim(
            @Param("activityId") Long activityId);

    /**
     * 消费端条件扣减数据库最终库存。
     */
    int decreaseActivityStock(
            @Param("activityId") Long activityId,
            @Param("requestedAt") LocalDateTime requestedAt);

    /**
     * 插入游客优惠券，并回填主键。
     */
    int insertUserCoupon(UserCoupon userCoupon);

    /**
     * 记录抢券数据库最终结果。
     */
    int insertClaimRequest(CouponClaimRequest claimRequest);

}
