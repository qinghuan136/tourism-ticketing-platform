package com.qinghuan.tickettype;

import com.qinghuan.auth.context.UserContext;
import com.qinghuan.auth.model.LoginUser;
import com.qinghuan.common.exception.BusinessException;
import com.qinghuan.common.exception.ErrorCode;
import com.qinghuan.pojo.dto.TicketTypeUpdateDTO;
import com.qinghuan.pojo.enums.AccountRole;
import com.qinghuan.pojo.enums.TicketTypeStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketTypeServiceImplTest {

    @Mock
    private TicketTypeMapper ticketTypeMapper;

    private TicketTypeServiceImpl ticketTypeService;
    private TicketTypeUpdateDTO updateDTO;

    @BeforeEach
    void setUp() {
        ticketTypeService = new TicketTypeServiceImpl(ticketTypeMapper);
        UserContext.set(new LoginUser(1L, "operator", AccountRole.OPERATOR, 10L));

        updateDTO = new TicketTypeUpdateDTO();
        updateDTO.setName("学生优惠票");
        updateDTO.setDescription("面向全日制在校学生");
        updateDTO.setAudienceRule("入场时须出示本人有效学生证");
        updateDTO.setBasePrice(new BigDecimal("25.00"));
        updateDTO.setStatus(TicketTypeStatus.ENABLED);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void updateTicketType_shouldUpdateCurrentVenueTicketType() {
        when(ticketTypeMapper.updateTicketType(12L, 10L, updateDTO)).thenReturn(1);

        ticketTypeService.updateTicketType(12L, updateDTO);

        verify(ticketTypeMapper).updateTicketType(12L, 10L, updateDTO);
    }

    @Test
    void updateTicketType_shouldThrowNotFound_whenTicketTypeIsOutsideCurrentVenue() {
        when(ticketTypeMapper.updateTicketType(12L, 10L, updateDTO)).thenReturn(0);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> ticketTypeService.updateTicketType(12L, updateDTO));

        assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void updateTicketType_shouldThrowConflict_whenNameAlreadyExists() {
        when(ticketTypeMapper.updateTicketType(12L, 10L, updateDTO))
                .thenThrow(new DuplicateKeyException("duplicate name"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> ticketTypeService.updateTicketType(12L, updateDTO));

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
    }
}
