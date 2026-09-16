package com.qinghuan.coupon;

import com.qinghuan.common.response.ApiResponse;
import com.qinghuan.pojo.remote.coupon.CouponOrderOperationRequest;
import com.qinghuan.pojo.remote.coupon.CouponOrderTryRequest;
import com.qinghuan.pojo.remote.coupon.CouponOrderTryResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 仅供订单服务调用的优惠券 TCC 接口。 */
@RestController
public class InternalCouponController {

    private final CouponOrderService couponOrderService;

    public InternalCouponController(CouponOrderService couponOrderService) {
        this.couponOrderService = couponOrderService;
    }

    @PostMapping("/internal/coupons/try-lock")
    public ApiResponse<CouponOrderTryResponse> tryLock(@RequestBody CouponOrderTryRequest request) {
        return ApiResponse.success(couponOrderService.tryLock(request));
    }

    @PostMapping("/internal/coupons/confirm")
    public ApiResponse<Void> confirm(@RequestBody CouponOrderOperationRequest request) {
        couponOrderService.confirm(request);
        return ApiResponse.success();
    }

    @PostMapping("/internal/coupons/cancel")
    public ApiResponse<Void> cancel(@RequestBody CouponOrderOperationRequest request) {
        couponOrderService.cancel(request);
        return ApiResponse.success();
    }

    @PostMapping("/internal/coupons/restore-after-refund")
    public ApiResponse<Void> restoreAfterRefund(@RequestBody CouponOrderOperationRequest request) {
        couponOrderService.restoreAfterRefund(request);
        return ApiResponse.success();
    }
}
