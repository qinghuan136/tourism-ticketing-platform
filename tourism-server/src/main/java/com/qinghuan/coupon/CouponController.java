package com.qinghuan.coupon;

import com.qinghuan.annotation.RequireRole;
import com.qinghuan.common.response.ApiResponse;
import com.qinghuan.pojo.dto.CouponActivityPageQueryDTO;
import com.qinghuan.pojo.dto.CouponActivityWriteDTO;
import com.qinghuan.pojo.dto.UserCouponQueryDTO;
import com.qinghuan.pojo.enums.AccountRole;
import com.qinghuan.pojo.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@Tag(name = "优惠券", description = "优惠券活动管理、游客异步领券与个人优惠券查询")
public class CouponController {

    private final CouponActivityService activityService;
    private final CouponQueryService queryService;
    private final CouponClaimService claimService;

    public CouponController(CouponActivityService activityService,
                            CouponQueryService queryService, CouponClaimService claimService) {
        this.activityService = activityService;
        this.queryService = queryService;
        this.claimService = claimService;
    }

    /** 运营端列表始终由 JWT 中的 venueId 限制数据范围。 */
    @GetMapping("/operator/coupon-activities")
    @RequireRole(AccountRole.OPERATOR)
    @Operation(summary = "分页查询优惠券活动")
    @SecurityRequirement(name = "BearerAuth")
    public ApiResponse<PageResult<CouponActivityVO>> pageActivities(
            @ParameterObject @Valid CouponActivityPageQueryDTO query) {
        return ApiResponse.success(activityService.pageActivities(query));
    }

    /** 创建动作只产生 DRAFT，不在 HTTP 请求内执行 Redis 预热。 */
    @PostMapping("/operator/coupon-activities")
    @ResponseStatus(HttpStatus.CREATED)
    @RequireRole(AccountRole.OPERATOR)
    @Operation(summary = "创建优惠券活动草稿")
    @SecurityRequirement(name = "BearerAuth")
    public ApiResponse<CouponActivityCreatedVO> createDraft(
            @Valid @RequestBody CouponActivityWriteDTO writeDTO) {
        return ApiResponse.success(activityService.createDraft(writeDTO));
    }

    /** 查询活动详情；跨景点活动由 Service 按不存在处理。 */
    @GetMapping("/operator/coupon-activities/{activityId}")
    @RequireRole(AccountRole.OPERATOR)
    @Operation(summary = "获取优惠券活动详情")
    @SecurityRequirement(name = "BearerAuth")
    public ApiResponse<CouponActivityVO> getActivity(
            @PathVariable @Positive(message = "活动ID必须为正数") Long activityId) {
        return ApiResponse.success(activityService.getActivity(activityId));
    }

    /** 发布后活动规则被冻结，因此只有草稿支持整体修改。 */
    @PutMapping("/operator/coupon-activities/{activityId}")
    @RequireRole(AccountRole.OPERATOR)
    @Operation(summary = "修改优惠券活动草稿")
    @SecurityRequirement(name = "BearerAuth")
    public ApiResponse<Void> updateDraft(
            @PathVariable @Positive(message = "活动ID必须为正数") Long activityId,
            @Valid @RequestBody CouponActivityWriteDTO writeDTO) {
        activityService.updateDraft(activityId, writeDTO);
        return ApiResponse.success();
    }

    /** 发布只完成数据库状态转换，自动预热属于后续核心链路。 */
    @PostMapping("/operator/coupon-activities/{activityId}/publish")
    @RequireRole(AccountRole.OPERATOR)
    @Operation(summary = "发布优惠券活动")
    @SecurityRequirement(name = "BearerAuth")
    public ApiResponse<Void> publish(
            @PathVariable @Positive(message = "活动ID必须为正数") Long activityId) {
        activityService.publish(activityId);
        return ApiResponse.success();
    }

    /** 取消仅关闭后续领取，已经发出的 user_coupon 继续有效。 */
    @PostMapping("/operator/coupon-activities/{activityId}/cancel")
    @RequireRole(AccountRole.OPERATOR)
    @Operation(summary = "取消优惠券活动")
    @SecurityRequirement(name = "BearerAuth")
    public ApiResponse<Void> cancel(
            @PathVariable @Positive(message = "活动ID必须为正数") Long activityId) {
        activityService.cancel(activityId);
        return ApiResponse.success();
    }

    /** 公开目录接口放在 /public/** 下，因此允许未登录游客读取。 */
    @GetMapping("/public/venues/{venueId}/coupon-activities")
    @Operation(summary = "查询景点可领取优惠券活动")
    public ApiResponse<List<CatalogCouponActivityVO>> listCatalogActivities(
            @PathVariable @Positive(message = "景点ID必须为正数") Long venueId) {
        return ApiResponse.success(queryService.listCatalogActivities(venueId));
    }

    /** 异步提交后由游客轮询数据库最终处理结果。 */
    @GetMapping("/tourist/coupon-claims/{requestId}")
    @RequireRole(AccountRole.TOURIST)
    @Operation(summary = "查询异步抢券结果", description = "使用提交领券请求时返回的 requestId 轮询，直到状态变为 SUCCESS 或 FAILED")
    @SecurityRequirement(name = "BearerAuth")
    public ApiResponse<CouponClaimResultVO> getClaimResult(
            @PathVariable @NotBlank(message = "请求号不能为空") String requestId) {
        return ApiResponse.success(queryService.getClaimResult(requestId));
    }

    /** 只读取当前登录游客本人持有的优惠券。 */
    @GetMapping("/tourist/coupons")
    @RequireRole(AccountRole.TOURIST)
    @Operation(summary = "查询我的优惠券")
    @SecurityRequirement(name = "BearerAuth")
    public ApiResponse<List<UserCouponVO>> listMyCoupons(@ParameterObject @Valid UserCouponQueryDTO query) {
        return ApiResponse.success(queryService.listMyCoupons(query));
    }

    /**
     * Redis 接受请求后返回 PENDING。
     */
    @PostMapping("/tourist/coupon-activities/{activityId}/claims")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @RequireRole(AccountRole.TOURIST)
    @Operation(summary = "异步提交抢券请求", description = "请求被受理后返回 requestId 和 PENDING 状态，最终结果通过领券结果接口查询")
    @SecurityRequirement(name = "BearerAuth")
    public ApiResponse<CouponClaimAcceptedVO> claimCoupon(
            @PathVariable
            @Positive(message = "活动ID必须为正数")
            Long activityId) {

        return ApiResponse.success(
                claimService.claim(activityId)
        );
    }
}
