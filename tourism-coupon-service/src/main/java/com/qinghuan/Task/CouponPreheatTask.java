package com.qinghuan.Task;

import com.qinghuan.coupon.CouponPreheatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定期扫描并预热即将开始的优惠券活动。
 */
@Slf4j
@Component
public class CouponPreheatTask {

    private final CouponPreheatService couponPreheatService;

    public CouponPreheatTask(
            CouponPreheatService couponPreheatService) {
        this.couponPreheatService = couponPreheatService;
    }

    /**
     * 使用 fixedDelay：
     * 本轮执行结束后再等待指定时间，避免同一实例中的任务重叠。
     */
    @Scheduled(fixedDelay = 10000L)
    public void preheatUpcomingActivities() {
        try {
            int successCount =
                    couponPreheatService.preheatUpcomingActivities();

            if (successCount > 0) {
                log.info(
                        "本轮优惠券活动预热完成，成功数量={}",
                        successCount
                );
            }
        } catch (Exception exception) {
            /*
             * 查询数据库等批次级异常在这里记录。
             * 下一个调度周期会重新执行，不需要终止应用。
             */
            log.error("优惠券自动预热任务执行失败", exception);
        }
    }
}