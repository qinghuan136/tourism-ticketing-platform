package com.qinghuan.Task;

import com.qinghuan.booking.BookingService;
import com.qinghuan.common.constant.cacheKeys.LockConstant;
import com.qinghuan.pojo.entity.BookingOrder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class BookingTask {
    private final BookingService bookingService;
    private final RedissonClient redissonClient;
    private final long refundPendingSeconds;

    public BookingTask(
            BookingService bookingService,
            RedissonClient redissonClient,
            @Value("${app.refund.pending-seconds:30}") long refundPendingSeconds) {
        this.bookingService = bookingService;
        this.redissonClient = redissonClient;
        this.refundPendingSeconds = refundPendingSeconds;
    }

    /*
     * 每分钟清理一次超时未支付订单
     */
    @Scheduled(cron = "0 * * * * *")
    public void autoCancelOrder() {
        log.info("开始清理超时未支付订单");
        // 获取所有超时未支付订单
        List<BookingOrder> timeoutOrders = bookingService.listTimeoutOrders();
        for (BookingOrder order : timeoutOrders) {
            try {
                // 取消超时订单
                bookingService.cancelTimeoutOrder(order.getId());
            } catch (Exception e) {
                log.error("取消超时订单失败：{}，订单号：{}", e.getMessage(), order.getOrderNo());
            }
        }
    }

    /**
     * 对账长时间停留在 REFUNDING 的订单。
     * 查询和重试始终使用订单中已保存的同一个 refundNo。
     */
    @Scheduled(cron = "${app.refund.reconcile-cron:15 * * * * *}")
    public void reconcileRefunds() {
        LocalDateTime requestedBefore = LocalDateTime.now()
                .minusSeconds(refundPendingSeconds);
        List<BookingOrder> orders = bookingService.listRefundingOrders(requestedBefore);
        for (BookingOrder order : orders) {
            RLock lock = redissonClient.getLock(
                    LockConstant.LOCK_REFUND_RECONCILE_PREFIX + order.getId());

            // 其他实例正在对账同一笔订单时，本实例直接跳过。
            if (!lock.tryLock()) {
                continue;
            }

            try {
                bookingService.reconcileRefund(order.getId());
            } catch (Exception exception) {
                log.error("退款对账失败，refundNo={}", order.getRefundNo(), exception);
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
    }
}
