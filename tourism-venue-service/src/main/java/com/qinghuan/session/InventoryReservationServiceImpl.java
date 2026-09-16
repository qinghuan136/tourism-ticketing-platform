package com.qinghuan.session;

import com.qinghuan.common.exception.BusinessException;
import com.qinghuan.common.exception.ErrorCode;
import com.qinghuan.pojo.entity.InventoryReservation;
import com.qinghuan.pojo.entity.InventoryReservationOrder;
import com.qinghuan.pojo.enums.InventoryReservationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class InventoryReservationServiceImpl implements InventoryReservationService {

    private final InventoryReservationMapper reservationMapper;
    private final SessionInventoryService sessionInventoryService;

    public InventoryReservationServiceImpl(InventoryReservationMapper reservationMapper,
                                           SessionInventoryService sessionInventoryService) {
        this.reservationMapper = reservationMapper;
        this.sessionInventoryService = sessionInventoryService;
    }

    @Override
    @Transactional
    public void tryReserve(Long orderId, Long sessionId, Map<Long, Integer> ticketTypeQuantities) {
        int inserted = reservationMapper.insertOrderIfAbsent(
                orderId, sessionId, InventoryReservationStatus.RESERVED);
        InventoryReservationOrder reservationOrder = reservationMapper.findOrderForUpdate(orderId);

        if (inserted == 0) {
            if (reservationOrder.getStatus() == InventoryReservationStatus.CANCELED) {
                throw new BusinessException(ErrorCode.CONFLICT, "库存预留已取消，不能再次预留");
            }
            if (!matches(reservationOrder, sessionId, ticketTypeQuantities)) {
                throw new BusinessException(ErrorCode.CONFLICT, "同一订单的库存预留参数不一致");
            }
            // RESERVED 和 CONFIRMED 都说明该 orderId 已成功完成过 Try。
            return;
        }

        // 条件更新仍是防超卖的最终裁决；任一票种不足会回滚场次容量和预留记录。
        sessionInventoryService.reserveInventory(sessionId, ticketTypeQuantities);
        for (Map.Entry<Long, Integer> entry : ticketTypeQuantities.entrySet()) {
            reservationMapper.insertReservation(
                    orderId, entry.getKey(), entry.getValue(), InventoryReservationStatus.RESERVED);
        }
    }

    @Override
    @Transactional
    public void confirmReserve(Long orderId) {
        InventoryReservationOrder reservationOrder = reservationMapper.findOrderForUpdate(orderId);
        if (reservationOrder == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "库存预留不存在");
        }
        if (reservationOrder.getStatus() == InventoryReservationStatus.CONFIRMED) {
            return;
        }
        if (reservationOrder.getStatus() == InventoryReservationStatus.CANCELED) {
            throw new BusinessException(ErrorCode.CONFLICT, "已取消的库存预留不能确认");
        }
        reservationMapper.updateReservationStatus(orderId, InventoryReservationStatus.CONFIRMED);
        reservationMapper.updateOrderStatus(orderId, InventoryReservationStatus.CONFIRMED);
    }

    @Override
    @Transactional
    public void cancelReserve(Long orderId) {
        // 先写取消屏障；即使 Cancel 比 Try 先到，后续 Try 也会被拒绝。
        reservationMapper.insertCancelMarkerIfAbsent(orderId);
        InventoryReservationOrder reservationOrder = reservationMapper.findOrderForUpdate(orderId);
        if (reservationOrder.getStatus() == InventoryReservationStatus.CANCELED) {
            return;
        }
        if (reservationOrder.getStatus() == InventoryReservationStatus.CONFIRMED) {
            // Confirm 之后的库存释放属于订单取消/退款流程，不能由 TCC Cancel 重复归还。
            return;
        }

        List<InventoryReservation> reservations = reservationMapper.listReservationsForUpdate(orderId);
        Map<Long, Integer> ticketTypeQuantities = reservations.stream()
                .collect(java.util.stream.Collectors.toMap(
                        InventoryReservation::getSessionTicketTypeId,
                        InventoryReservation::getQuantity));
        sessionInventoryService.releaseInventory(reservationOrder.getSessionId(), ticketTypeQuantities);
        reservationMapper.updateReservationStatus(orderId, InventoryReservationStatus.CANCELED);
        reservationMapper.updateOrderStatus(orderId, InventoryReservationStatus.CANCELED);
    }

    private boolean matches(InventoryReservationOrder reservationOrder,
                            Long sessionId,
                            Map<Long, Integer> ticketTypeQuantities) {
        if (!sessionId.equals(reservationOrder.getSessionId())) {
            return false;
        }
        List<InventoryReservation> reservations = reservationMapper
                .listReservationsForUpdate(reservationOrder.getOrderId());
        if (reservations.size() != ticketTypeQuantities.size()) {
            return false;
        }
        return reservations.stream().allMatch(reservation ->
                ticketTypeQuantities.containsKey(reservation.getSessionTicketTypeId())
                        && reservation.getQuantity().equals(
                        ticketTypeQuantities.get(reservation.getSessionTicketTypeId())));
    }
}
