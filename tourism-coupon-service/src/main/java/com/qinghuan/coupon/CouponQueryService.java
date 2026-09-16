package com.qinghuan.coupon;

import com.qinghuan.pojo.dto.UserCouponQueryDTO;
import com.qinghuan.pojo.vo.CatalogCouponActivityVO;
import com.qinghuan.pojo.vo.CouponClaimResultVO;
import com.qinghuan.pojo.vo.UserCouponVO;

import java.time.LocalDateTime;
import java.util.List;

public interface CouponQueryService {
    /** 查询公开目录中已发布且尚未结束的活动。 */
    List<CatalogCouponActivityVO> listCatalogActivities(Long venueId);

    /** 按 requestId 查询当前游客自己的异步领取结果。 */
    CouponClaimResultVO getClaimResult(String requestId);

    /** 查询当前游客本人持有的优惠券。 */
    List<UserCouponVO> listMyCoupons(UserCouponQueryDTO query);

    /** 定时收口已经过期但仍为 AVAILABLE 的优惠券。 */
    int expireAvailableCoupons(LocalDateTime now);
}
