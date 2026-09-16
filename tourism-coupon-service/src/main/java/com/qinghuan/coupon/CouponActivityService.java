package com.qinghuan.coupon;

import com.qinghuan.pojo.dto.CouponActivityPageQueryDTO;
import com.qinghuan.pojo.dto.CouponActivityWriteDTO;
import com.qinghuan.pojo.vo.CouponActivityCreatedVO;
import com.qinghuan.pojo.vo.CouponActivityVO;
import com.qinghuan.pojo.vo.PageResult;

import java.time.LocalDateTime;

public interface CouponActivityService {
    /** 按当前运营者绑定的景点分页查询活动。 */
    PageResult<CouponActivityVO> pageActivities(CouponActivityPageQueryDTO query);

    /** 创建活动草稿；发布和 Redis 预热均不在此步骤执行。 */
    CouponActivityCreatedVO createDraft(CouponActivityWriteDTO writeDTO);

    /** 查询当前运营者景点范围内的活动详情。 */
    CouponActivityVO getActivity(Long activityId);

    /** 修改草稿并重置其数据库预热标记。 */
    void updateDraft(Long activityId, CouponActivityWriteDTO writeDTO);

    /** 将草稿发布为 PUBLISHED，等待核心链路在开抢前预热。 */
    void publish(Long activityId);

    /** 取消草稿或已发布活动，不作废已经发放的游客优惠券。 */
    void cancel(Long activityId);

    /** 定时把超过领取结束时间的已发布活动收口为 ENDED。 */
    int endExpiredActivities(LocalDateTime now);
}
