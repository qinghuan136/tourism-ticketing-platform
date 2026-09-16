package com.qinghuan.Task;

import com.qinghuan.booking.BookingCouponTccService;
import com.qinghuan.pojo.entity.BookingCouponTccOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 重放优惠券 Confirm/Cancel，避免网络故障遗留 LOCKED 状态。 */
@Slf4j
@Component
public class BookingCouponTccTask {

    private final BookingCouponTccService tccService;

    public BookingCouponTccTask(BookingCouponTccService tccService) {
        this.tccService = tccService;
    }

    @Scheduled(fixedDelayString = "${app.coupon.order-tcc.retry-delay:10000}")
    public void retryPendingOperations() {
        for (BookingCouponTccOperation operation : tccService.listPending()) {
            try {
                tccService.retry(operation);
            } catch (RuntimeException exception) {
                log.error("优惠券 TCC 二阶段重试失败, orderId={}", operation.getOrderId(), exception);
            }
        }
    }
}
