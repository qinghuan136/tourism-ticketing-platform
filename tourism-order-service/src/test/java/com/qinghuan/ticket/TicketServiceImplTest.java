package com.qinghuan.ticket;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qinghuan.auth.context.UserContext;
import com.qinghuan.auth.model.LoginUser;
import com.qinghuan.common.exception.BusinessException;
import com.qinghuan.common.exception.ErrorCode;
import com.qinghuan.pojo.dto.TicketPageQueryDTO;
import com.qinghuan.pojo.enums.AccountRole;
import com.qinghuan.pojo.enums.TicketStatus;
import com.qinghuan.pojo.vo.PageResult;
import com.qinghuan.pojo.vo.TicketVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    @Mock
    private TicketMapper ticketMapper;

    private TicketServiceImpl ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketServiceImpl(ticketMapper);
        UserContext.set(new LoginUser(7L, "tourist", AccountRole.TOURIST, null));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
        PageHelper.clearPage();
    }

    @Test
    void pageMyTickets_shouldOnlyQueryCurrentTourist() {
        TicketPageQueryDTO query = new TicketPageQueryDTO();
        query.setStatus(TicketStatus.VALID);
        TicketVO ticket = new TicketVO();
        ticket.setId(701L);
        Page<TicketVO> page = new Page<>(1, 20);
        page.add(ticket);
        page.setTotal(1);
        when(ticketMapper.listMyTickets(7L, query)).thenReturn(page);

        PageResult<TicketVO> result = ticketService.pageMyTickets(query);

        assertEquals(1, result.total());
        assertEquals(701L, result.items().get(0).getId());
    }

    @Test
    void getMyTicket_shouldReturnCurrentTouristTicket() {
        TicketVO ticket = new TicketVO();
        ticket.setId(701L);
        when(ticketMapper.findMyTicket(701L, 7L)).thenReturn(ticket);

        assertEquals(701L, ticketService.getMyTicket(701L).getId());
    }

    @Test
    void getMyTicket_shouldReturnNotFoundWhenTicketIsOutsideCurrentTourist() {
        when(ticketMapper.findMyTicket(701L, 7L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class, () -> ticketService.getMyTicket(701L));

        assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
    }
}
