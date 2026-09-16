package com.qinghuan.coupon;

import com.qinghuan.auth.context.UserContext;
import com.qinghuan.common.exception.BusinessException;
import com.qinghuan.common.exception.ErrorCode;
import com.qinghuan.pojo.dto.UserCouponQueryDTO;
import com.qinghuan.pojo.vo.CatalogCouponActivityVO;
import com.qinghuan.pojo.vo.CouponClaimResultVO;
import com.qinghuan.pojo.vo.UserCouponVO;
import com.qinghuan.pojo.enums.CouponClaimStatus;
import com.qinghuan.pojo.remote.venue.VenueSummaryDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.qinghuan.common.constant.cacheKeys.CouponConstant;

@Service
public class CouponQueryServiceImpl implements CouponQueryService {

    private final CouponMapper couponMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final VenueClient venueClient;

    public CouponQueryServiceImpl(CouponMapper couponMapper,
                                  StringRedisTemplate stringRedisTemplate,
                                  VenueClient venueClient) {
        this.couponMapper = couponMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.venueClient = venueClient;
    }

    @Override
    public List<CatalogCouponActivityVO> listCatalogActivities(Long venueId) {
        VenueSummaryDTO venue = venueClient.getVenueSummary(venueId).data();
        if (!venue.enabled()) {
            return List.of();
        }
        // 当前时间只生成一次，保证同一条查询中的状态和结束时间判断使用相同基准。
        return couponMapper.listCatalogActivities(venueId, LocalDateTime.now());
    }

    @Override
    public CouponClaimResultVO getClaimResult(String requestId) {
        Long userId = UserContext.getRequired().userId();
        Map<Object, Object> cached = stringRedisTemplate.opsForHash()
                .entries(CouponConstant.claimResultKey(requestId));
        if (!cached.isEmpty()) {
            // Redis 结果同样校验所属游客，不能只依赖难以猜测的 requestId。
            if (!userId.toString().equals(String.valueOf(cached.get("userId")))) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "抢券请求不存在");
            }
            return toClaimResult(requestId, cached);
        }

        // userId 放在 SQL 条件中，避免游客通过猜测 requestId 查询他人的领取结果。
        CouponClaimResultVO result = couponMapper.findClaimResult(
                requestId, userId);
        if (result == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "抢券请求不存在");
        }
        return result;
    }

    private CouponClaimResultVO toClaimResult(
            String requestId, Map<Object, Object> cached) {
        CouponClaimResultVO result = new CouponClaimResultVO();
        result.setRequestId(requestId);
        result.setActivityId(Long.valueOf(cached.get("activityId").toString()));
        result.setStatus(CouponClaimStatus.valueOf(cached.get("status").toString()));
        if (cached.get("userCouponId") != null) {
            result.setUserCouponId(Long.valueOf(cached.get("userCouponId").toString()));
        }
        if (cached.get("failureReason") != null) {
            result.setFailureReason(cached.get("failureReason").toString());
        }
        return result;
    }

    @Override
    @Transactional
    public List<UserCouponVO> listMyCoupons(UserCouponQueryDTO query) {
        // 查询前同步收口到期状态，避免定时任务的一分钟延迟展示出“可用”过期券。
        couponMapper.expireAvailableCoupons(LocalDateTime.now());
        return couponMapper.listUserCoupons(UserContext.getRequired().userId(), query);
    }

    @Override
    public int expireAvailableCoupons(LocalDateTime now) {
        // LOCKED 券由其关联订单的取消、超时或退款流程处理，任务只更新 AVAILABLE。
        return couponMapper.expireAvailableCoupons(now);
    }
}
