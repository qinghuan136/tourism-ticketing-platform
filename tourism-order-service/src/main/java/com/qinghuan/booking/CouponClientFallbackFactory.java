package com.qinghuan.booking;

import com.qinghuan.common.exception.BusinessException;
import com.qinghuan.common.response.ApiResponse;
import com.qinghuan.pojo.remote.coupon.CouponOrderOperationRequest;
import com.qinghuan.pojo.remote.coupon.CouponOrderTryRequest;
import com.qinghuan.pojo.remote.coupon.CouponOrderTryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/** 熔断或调用失败时，优惠券 TCC 操作交给现有的 Cancel/重试路径处理。 */
@Component
public class CouponClientFallbackFactory implements FallbackFactory<CouponClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CouponClientFallbackFactory.class);

    @Override
    public CouponClient create(Throwable cause) {
        LOGGER.warn("coupon-service 调用失败或已熔断: {}", cause.toString());
        return new CouponClient() {
            @Override
            public ApiResponse<CouponOrderTryResponse> tryLock(CouponOrderTryRequest request) {
                throw unavailable();
            }

            @Override
            public ApiResponse<Void> confirm(CouponOrderOperationRequest request) {
                throw unavailable();
            }

            @Override
            public ApiResponse<Void> cancel(CouponOrderOperationRequest request) {
                throw unavailable();
            }

            @Override
            public ApiResponse<Void> restoreAfterRefund(CouponOrderOperationRequest request) {
                throw unavailable();
            }

            private RemoteServiceUnavailableException unavailable() {
                if (cause instanceof BusinessException businessException) {
                    // 下游业务校验失败应原样交给订单编排处理。
                    throw businessException;
                }
                return new RemoteServiceUnavailableException("优惠券服务暂时不可用", cause);
            }
        };
    }
}
