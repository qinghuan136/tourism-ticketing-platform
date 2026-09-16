package com.qinghuan.Task;

import com.qinghuan.booking.BookingInventoryTccService;
import com.qinghuan.common.constant.cacheKeys.LockConstant;
import com.qinghuan.pojo.entity.BookingInventoryTccOperation;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 重试网络异常后仍停留在二阶段的库存 TCC 操作。 */
@Slf4j
@Component
public class BookingInventoryTccTask {

    private final BookingInventoryTccService tccService;
    private final RedissonClient redisson;

    public BookingInventoryTccTask(BookingInventoryTccService tccService,
                                   RedissonClient redisson) {
        this.tccService = tccService;
        this.redisson = redisson;
    }

    @Scheduled(fixedDelayString = "${app.inventory-tcc.retry-delay:10000}")
    public void retryPendingOperations() {
        for (BookingInventoryTccOperation operation : tccService.listPending()) {
            RLock lock = redisson.getLock(LockConstant.LOCK_INVENTORY_TCC_PREFIX + operation.getOrderId());
            if (!lock.tryLock()) {
                continue;
            }
            try {
                tccService.retry(operation);
            } catch (RuntimeException exception) {
                log.error("库存 TCC 二阶段重试失败, orderId={}", operation.getOrderId(), exception);
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
    }
}
