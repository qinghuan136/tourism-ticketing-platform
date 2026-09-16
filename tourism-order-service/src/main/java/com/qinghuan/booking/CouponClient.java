package com.qinghuan.booking;

import com.qinghuan.common.response.ApiResponse;
import com.qinghuan.pojo.remote.coupon.CouponOrderOperationRequest;
import com.qinghuan.pojo.remote.coupon.CouponOrderTryRequest;
import com.qinghuan.pojo.remote.coupon.CouponOrderTryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** 订单域到优惠券域的粗粒度 TCC 调用。 */
@FeignClient(name = "coupon-service", fallbackFactory = CouponClientFallbackFactory.class)
public interface CouponClient {

    @PostMapping("/internal/coupons/try-lock")
    ApiResponse<CouponOrderTryResponse> tryLock(@RequestBody CouponOrderTryRequest request);

    @PostMapping("/internal/coupons/confirm")
    ApiResponse<Void> confirm(@RequestBody CouponOrderOperationRequest request);

    @PostMapping("/internal/coupons/cancel")
    ApiResponse<Void> cancel(@RequestBody CouponOrderOperationRequest request);

    @PostMapping("/internal/coupons/restore-after-refund")
    ApiResponse<Void> restoreAfterRefund(@RequestBody CouponOrderOperationRequest request);
}
