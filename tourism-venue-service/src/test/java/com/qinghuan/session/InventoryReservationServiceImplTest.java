package com.qinghuan.session;

import com.qinghuan.common.exception.BusinessException;
import com.qinghuan.pojo.entity.InventoryReservation;
import com.qinghuan.pojo.entity.InventoryReservationOrder;
import com.qinghuan.pojo.enums.InventoryReservationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryReservationServiceImplTest {

    @Mock
    private InventoryReservationMapper reservationMapper;
    @Mock
    private SessionInventoryService sessionInventoryService;

    @Test
    void shouldDeductAndCreateReservationOnFirstTry() {
        InventoryReservationOrder order = reservationOrder(InventoryReservationStatus.RESERVED, 21L);
        when(reservationMapper.insertOrderIfAbsent(9001L, 21L, InventoryReservationStatus.RESERVED))
                .thenReturn(1);
        when(reservationMapper.findOrderForUpdate(9001L)).thenReturn(order);

        service().tryReserve(9001L, 21L, Map.of(301L, 2));

        verify(sessionInventoryService).reserveInventory(21L, Map.of(301L, 2));
        verify(reservationMapper).insertReservation(
                9001L, 301L, 2, InventoryReservationStatus.RESERVED);
    }

    @Test
    void shouldNotDeductAgainForRepeatedTry() {
        InventoryReservationOrder order = reservationOrder(InventoryReservationStatus.RESERVED, 21L);
        when(reservationMapper.insertOrderIfAbsent(9001L, 21L, InventoryReservationStatus.RESERVED))
                .thenReturn(0);
        when(reservationMapper.findOrderForUpdate(9001L)).thenReturn(order);
        when(reservationMapper.listReservationsForUpdate(9001L))
                .thenReturn(List.of(reservation(9001L, 301L, 2, InventoryReservationStatus.RESERVED)));

        assertDoesNotThrow(() -> service().tryReserve(9001L, 21L, Map.of(301L, 2)));

        verify(sessionInventoryService, never()).reserveInventory(anyLong(), any());
    }

    @Test
    void shouldRejectTryAfterCancelBarrier() {
        when(reservationMapper.insertOrderIfAbsent(9001L, 21L, InventoryReservationStatus.RESERVED))
                .thenReturn(0);
        when(reservationMapper.findOrderForUpdate(9001L))
                .thenReturn(reservationOrder(InventoryReservationStatus.CANCELED, null));

        assertThrows(BusinessException.class,
                () -> service().tryReserve(9001L, 21L, Map.of(301L, 1)));
        verify(sessionInventoryService, never()).reserveInventory(anyLong(), any());
    }

    @Test
    void shouldRestoreOnlyOnceWhenCancelIsRepeated() {
        when(reservationMapper.findOrderForUpdate(9001L))
                .thenReturn(reservationOrder(InventoryReservationStatus.RESERVED, 21L),
                        reservationOrder(InventoryReservationStatus.CANCELED, 21L));
        when(reservationMapper.listReservationsForUpdate(9001L))
                .thenReturn(List.of(reservation(9001L, 301L, 2, InventoryReservationStatus.RESERVED)));

        service().cancelReserve(9001L);
        service().cancelReserve(9001L);

        verify(sessionInventoryService).releaseInventory(21L, Map.of(301L, 2));
        verify(reservationMapper).updateReservationStatus(9001L, InventoryReservationStatus.CANCELED);
        verify(reservationMapper).updateOrderStatus(9001L, InventoryReservationStatus.CANCELED);
    }

    @Test
    void shouldTreatRepeatedConfirmAsSuccess() {
        when(reservationMapper.findOrderForUpdate(9001L))
                .thenReturn(reservationOrder(InventoryReservationStatus.CONFIRMED, 21L));

        assertDoesNotThrow(() -> service().confirmReserve(9001L));

        verify(reservationMapper, never()).updateReservationStatus(anyLong(), any());
    }

    private InventoryReservationServiceImpl service() {
        return new InventoryReservationServiceImpl(reservationMapper, sessionInventoryService);
    }

    private InventoryReservationOrder reservationOrder(InventoryReservationStatus status, Long sessionId) {
        InventoryReservationOrder order = new InventoryReservationOrder();
        order.setOrderId(9001L);
        order.setSessionId(sessionId);
        order.setStatus(status);
        return order;
    }

    private InventoryReservation reservation(Long orderId, Long ticketTypeId,
                                             Integer quantity, InventoryReservationStatus status) {
        InventoryReservation reservation = new InventoryReservation();
        reservation.setOrderId(orderId);
        reservation.setSessionTicketTypeId(ticketTypeId);
        reservation.setQuantity(quantity);
        reservation.setStatus(status);
        return reservation;
    }
}
