package com.qinghuan.coupon;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qinghuan.auth.context.UserContext;
import com.qinghuan.common.exception.BusinessException;
import com.qinghuan.common.exception.ErrorCode;
import com.qinghuan.pojo.dto.CouponActivityPageQueryDTO;
import com.qinghuan.pojo.dto.CouponActivityWriteDTO;
import com.qinghuan.pojo.entity.CouponActivity;
import com.qinghuan.pojo.enums.CouponActivityStatus;
import com.qinghuan.pojo.remote.venue.VenueSummaryDTO;
import com.qinghuan.pojo.vo.CouponActivityCreatedVO;
import com.qinghuan.pojo.vo.CouponActivityVO;
import com.qinghuan.pojo.vo.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

@Service
public class CouponActivityServiceImpl implements CouponActivityService {

    private final CouponMapper couponMapper;
    private final CouponPreheatService preheatService;
    private final VenueClient venueClient;

    public CouponActivityServiceImpl(CouponMapper couponMapper,
                                     CouponPreheatService preheatService,
                                     VenueClient venueClient) {
        this.couponMapper = couponMapper;
        this.preheatService = preheatService;
        this.venueClient = venueClient;
    }

    @Override
    public PageResult<CouponActivityVO> pageActivities(CouponActivityPageQueryDTO query) {
        // venueId 只能来自登录上下文，不能允许客户端借查询条件读取其他景点的数据。
        PageHelper.startPage(query.getPage(), query.getSize());
        Page<CouponActivityVO> page = (Page<CouponActivityVO>) couponMapper.listActivities(
                UserContext.getRequired().venueId(), query);
        return new PageResult<>(page.getResult(), page.getTotal(), page.getPageNum(), page.getPageSize());
    }

    @Override
    @Transactional
    public CouponActivityCreatedVO createDraft(CouponActivityWriteDTO writeDTO) {
        validateRules(writeDTO);
        CouponActivity activity = buildActivity(writeDTO);

        // 景点和创建人属于权限信息，由服务端登录上下文决定。
        activity.setVenueId(UserContext.getRequired().venueId());
        activity.setVenueNameSnapshot(getCurrentVenueSummary().venueName());
        activity.setCreatedBy(UserContext.getRequired().userId());

        // 草稿尚未发生领取，数据库剩余库存等于发行量，且不应存在 Redis 预热数据。
        activity.setRemainingStock(writeDTO.getTotalStock());
        activity.setStatus(CouponActivityStatus.DRAFT);
        activity.setCacheReady(false);
        couponMapper.insertActivity(activity);
        return new CouponActivityCreatedVO(activity.getId(), activity.getStatus());
    }

    @Override
    public CouponActivityVO getActivity(Long activityId) {
        return getCurrentVenueActivity(activityId);
    }

    @Override
    @Transactional
    public void updateDraft(Long activityId, CouponActivityWriteDTO writeDTO) {
        CouponActivityVO current = getCurrentVenueActivity(activityId);
        if (current.getStatus() != CouponActivityStatus.DRAFT) {
            throw new BusinessException(ErrorCode.CONFLICT, "只有草稿活动可以修改");
        }
        validateRules(writeDTO);

        CouponActivity activity = buildActivity(writeDTO);
        activity.setId(activityId);
        activity.setVenueId(UserContext.getRequired().venueId());
        // 草稿没有领取记录，修改发行量时可以直接同步重置数据库剩余库存。
        activity.setRemainingStock(writeDTO.getTotalStock());
        // SQL 再次限制 status=DRAFT，防止校验后活动被并发发布却仍遭覆盖。
        if (couponMapper.updateDraftActivity(activity) == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "活动状态已发生变化，请刷新后重试");
        }
    }

    @Override
    @Transactional
    public void publish(Long activityId) {
        CouponActivityVO activity = getCurrentVenueActivity(activityId);
        if (activity.getStatus() != CouponActivityStatus.DRAFT) {
            throw new BusinessException(ErrorCode.CONFLICT, "只有草稿活动可以发布");
        }
        if (!LocalDateTime.now().isBefore(activity.getClaimEndAt())) {
            throw new BusinessException(ErrorCode.CONFLICT, "领取结束时间必须晚于当前时间");
        }
        // 状态更新携带原状态，由数据库裁决重复发布或发布/取消竞争。
        updateStatus(activityId, CouponActivityStatus.DRAFT, CouponActivityStatus.PUBLISHED);

        // 此处刻意不写 Redis：发布后的 cacheReady 仍为 false，自动预热链路由核心功能完成。
    }

    @Override
    @Transactional
    public void cancel(Long activityId) {
        CouponActivityVO activity = getCurrentVenueActivity(activityId);
        if (activity.getStatus() != CouponActivityStatus.DRAFT
                && activity.getStatus() != CouponActivityStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.CONFLICT, "当前活动状态不能取消");
        }
        updateStatus(activityId, activity.getStatus(), CouponActivityStatus.CANCELLED);

        // 数据库取消成功后再关闭 Redis，避免事务回滚却提前清掉正常活动。
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        preheatService.disableActivityCache(activityId);
                    }
                }
        );
    }

    @Override
    public int endExpiredActivities(LocalDateTime now) {
        // 即使定时任务晚执行，抢券入口仍必须独立校验 claimEndAt，不能只依赖 ENDED 状态。
        return couponMapper.endExpiredActivities(now);
    }

    private CouponActivityVO getCurrentVenueActivity(Long activityId) {
        // ID 与 venueId 一起查询，跨景点访问统一表现为资源不存在。
        CouponActivityVO activity = couponMapper.findActivity(
                activityId, UserContext.getRequired().venueId());
        if (activity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "优惠券活动不存在");
        }
        return activity;
    }

    private void updateStatus(Long activityId,
                              CouponActivityStatus oldStatus,
                              CouponActivityStatus newStatus) {
        // 受影响行数为 0 表示状态已被其他事务改变，不允许静默覆盖。
        int updated = couponMapper.updateActivityStatus(
                activityId, UserContext.getRequired().venueId(), oldStatus, newStatus);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "活动状态已发生变化，请刷新后重试");
        }
    }

    /** 校验只能由单字段注解无法表达的金额和时间关系。 */
    private void validateRules(CouponActivityWriteDTO writeDTO) {
        if (writeDTO.getThresholdAmount().compareTo(writeDTO.getDiscountAmount()) < 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "使用门槛不能小于优惠金额");
        }
        if (!writeDTO.getClaimStartAt().isBefore(writeDTO.getClaimEndAt())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "领取开始时间必须早于结束时间");
        }
        if (!writeDTO.getValidFrom().isBefore(writeDTO.getValidUntil())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "优惠券生效时间必须早于失效时间");
        }
        if (!writeDTO.getValidUntil().isAfter(writeDTO.getClaimStartAt())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "优惠券失效时间必须晚于领取开始时间");
        }
    }

    private CouponActivity buildActivity(CouponActivityWriteDTO writeDTO) {
        // 这里只复制运营者可编辑的规则字段，状态、归属和库存由业务方法统一设置。
        CouponActivity activity = new CouponActivity();
        activity.setName(writeDTO.getName().trim());
        activity.setThresholdAmount(writeDTO.getThresholdAmount());
        activity.setDiscountAmount(writeDTO.getDiscountAmount());
        activity.setTotalStock(writeDTO.getTotalStock());
        activity.setClaimStartAt(writeDTO.getClaimStartAt());
        activity.setClaimEndAt(writeDTO.getClaimEndAt());
        activity.setValidFrom(writeDTO.getValidFrom());
        activity.setValidUntil(writeDTO.getValidUntil());
        return activity;
    }

    private VenueSummaryDTO getCurrentVenueSummary() {
        VenueSummaryDTO venue = venueClient.getVenueSummary(UserContext.getRequired().venueId()).data();
        if (!venue.enabled()) {
            throw new BusinessException(ErrorCode.CONFLICT, "当前景点不可发布优惠券活动");
        }
        return venue;
    }
}
