package com.qinghuan.booking;

import com.qinghuan.pojo.entity.BookingInventoryTccOperation;
import com.qinghuan.pojo.enums.BookingInventoryTccStatus;
import com.qinghuan.pojo.remote.venue.InventoryReservationOperationRequest;
import com.qinghuan.pojo.remote.venue.InventoryTryReserveRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/** 订单服务侧的 TCC 协调器；状态落库后再发送二阶段远程调用。 */
@Service
public class BookingInventoryTccService {

    private final BookingInventoryTccMapper tccMapper;
    private final VenueClient venueClient;

    public BookingInventoryTccService(BookingInventoryTccMapper tccMapper,
                                      VenueClient venueClient) {
        this.tccMapper = tccMapper;
        this.venueClient = venueClient;
    }

    /** Try 之前先持久化协调记录，网络超时后也能确定性执行 Cancel。 */
    @Transactional
    public void start(Long orderId, Long sessionId) {
        tccMapper.insert(orderId, sessionId, BookingInventoryTccStatus.TRYING);
    }

    public void tryReserve(Long orderId, Long sessionId, Map<Long, Integer> ticketTypeQuantities) {
        venueClient.tryReserve(new InventoryTryReserveRequest(orderId, sessionId, ticketTypeQuantities));
    }

    /** 此方法加入订单的本地事务；事务提交后状态才会成为 CONFIRMING。 */
    @Transactional
    public void markConfirming(Long orderId) {
        tccMapper.updateStatus(orderId, BookingInventoryTccStatus.TRYING,
                BookingInventoryTccStatus.CONFIRMING);
    }

    /** 本地事务回滚后调用，随后才能安全执行远程 Cancel。 */
    @Transactional
    public void markCanceling(Long orderId) {
        tccMapper.updateStatus(orderId, BookingInventoryTccStatus.TRYING,
                BookingInventoryTccStatus.CANCELING);
    }

    public void confirm(Long orderId) {
        venueClient.confirmReserve(new InventoryReservationOperationRequest(orderId));
        tccMapper.updateStatus(orderId, BookingInventoryTccStatus.CONFIRMING,
                BookingInventoryTccStatus.CONFIRMED);
    }

    public void cancel(Long orderId) {
        venueClient.cancelReserve(new InventoryReservationOperationRequest(orderId));
        tccMapper.updateStatus(orderId, BookingInventoryTccStatus.CANCELING,
                BookingInventoryTccStatus.CANCELED);
    }

    public List<BookingInventoryTccOperation> listPending() {
        return tccMapper.listPending();
    }

    /** 定时任务根据已落库的二阶段状态重放幂等 Confirm 或 Cancel。 */
    public void retry(BookingInventoryTccOperation operation) {
        if (operation.getStatus() == BookingInventoryTccStatus.CONFIRMING) {
            confirm(operation.getOrderId());
        } else if (operation.getStatus() == BookingInventoryTccStatus.CANCELING) {
            cancel(operation.getOrderId());
        }
    }
}
