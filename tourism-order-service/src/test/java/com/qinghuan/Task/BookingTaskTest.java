package com.qinghuan.Task;

import com.qinghuan.booking.BookingService;
import com.qinghuan.common.constant.cacheKeys.LockConstant;
import com.qinghuan.pojo.entity.BookingOrder;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingTaskTest {

    private final BookingService bookingService = mock(BookingService.class);
    private final RedissonClient redissonClient = mock(RedissonClient.class);
    private final RLock lock = mock(RLock.class);
    private final BookingTask task = new BookingTask(
            bookingService, redissonClient, 30);

    @Test
    void reconcileRefunds_shouldSkipOrderLockedByAnotherInstance() {
        BookingOrder order = refundingOrder(1L);
        when(bookingService.listRefundingOrders(any(LocalDateTime.class)))
                .thenReturn(List.of(order));
        when(redissonClient.getLock(
                LockConstant.LOCK_REFUND_RECONCILE_PREFIX + order.getId()))
                .thenReturn(lock);
        when(lock.tryLock()).thenReturn(false);

        task.reconcileRefunds();

        verify(bookingService, never()).reconcileRefund(order.getId());
    }

    @Test
    void reconcileRefunds_shouldProcessAndReleaseAcquiredLock() {
        BookingOrder order = refundingOrder(2L);
        when(bookingService.listRefundingOrders(any(LocalDateTime.class)))
                .thenReturn(List.of(order));
        when(redissonClient.getLock(
                LockConstant.LOCK_REFUND_RECONCILE_PREFIX + order.getId()))
                .thenReturn(lock);
        when(lock.tryLock()).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        task.reconcileRefunds();

        verify(bookingService).reconcileRefund(order.getId());
        verify(lock).unlock();
    }

    private BookingOrder refundingOrder(Long orderId) {
        BookingOrder order = new BookingOrder();
        order.setId(orderId);
        return order;
    }
}
