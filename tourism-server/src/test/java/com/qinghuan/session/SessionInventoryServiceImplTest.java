package com.qinghuan.session;

import com.qinghuan.common.exception.BusinessException;
import com.qinghuan.common.exception.ErrorCode;
import com.qinghuan.pojo.vo.SessionTicketTypeVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionInventoryServiceImplTest {

    @Mock
    private SessionMapper sessionMapper;

    @Test
    void shouldReturnAllSelectedOrderableTicketTypes() {
        SessionInventoryService service = new SessionInventoryServiceImpl(sessionMapper);
        List<Long> ids = List.of(301L, 302L);
        List<SessionTicketTypeVO> ticketTypes = List.of(ticketType(301L), ticketType(302L));
        when(sessionMapper.listOrderableTicketTypes(21L, ids)).thenReturn(ticketTypes);

        List<SessionTicketTypeVO> result =
                service.getOrderableTicketTypes(21L, ids);

        assertEquals(ticketTypes, result);
    }

    @Test
    void shouldRejectUnavailableTicketType() {
        SessionInventoryService service = new SessionInventoryServiceImpl(sessionMapper);
        List<Long> ids = List.of(301L, 302L);
        when(sessionMapper.listOrderableTicketTypes(21L, ids))
                .thenReturn(List.of(ticketType(301L)));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.getOrderableTicketTypes(21L, ids));

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
    }

    @Test
    void shouldReserveSessionAndTicketTypeInventoryInFixedOrder() {
        SessionInventoryService service = new SessionInventoryServiceImpl(sessionMapper);
        when(sessionMapper.deductSessionCapacity(21L, 3)).thenReturn(1);
        when(sessionMapper.deductTicketTypeQuantity(21L, 301L, 2)).thenReturn(1);
        when(sessionMapper.deductTicketTypeQuantity(21L, 302L, 1)).thenReturn(1);

        service.reserveInventory(21L, Map.of(302L, 1, 301L, 2));

        InOrder inOrder = inOrder(sessionMapper);
        inOrder.verify(sessionMapper).deductSessionCapacity(21L, 3);
        inOrder.verify(sessionMapper).deductTicketTypeQuantity(21L, 301L, 2);
        inOrder.verify(sessionMapper).deductTicketTypeQuantity(21L, 302L, 1);
    }

    @Test
    void shouldReleaseSessionAndTicketTypeInventory() {
        SessionInventoryService service = new SessionInventoryServiceImpl(sessionMapper);
        when(sessionMapper.restoreSessionCapacity(21L, 2)).thenReturn(1);
        when(sessionMapper.restoreTicketTypeQuantity(21L, 301L, 2)).thenReturn(1);

        service.releaseInventory(21L, Map.of(301L, 2));

        InOrder inOrder = inOrder(sessionMapper);
        inOrder.verify(sessionMapper).restoreSessionCapacity(21L, 2);
        inOrder.verify(sessionMapper).restoreTicketTypeQuantity(21L, 301L, 2);
    }

    private SessionTicketTypeVO ticketType(Long id) {
        SessionTicketTypeVO ticketType = new SessionTicketTypeVO();
        ticketType.setSessionTicketTypeId(id);
        return ticketType;
    }
}
