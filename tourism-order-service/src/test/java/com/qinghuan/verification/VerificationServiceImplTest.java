package com.qinghuan.verification;

import com.qinghuan.auth.context.UserContext;
import com.qinghuan.auth.model.LoginUser;
import com.qinghuan.common.exception.BusinessException;
import com.qinghuan.common.exception.ErrorCode;
import com.qinghuan.pojo.dto.VerificationRequestDTO;
import com.qinghuan.pojo.entity.VerificationRecord;
import com.qinghuan.pojo.enums.AccountRole;
import com.qinghuan.pojo.enums.TicketStatus;
import com.qinghuan.pojo.enums.VerificationResult;
import com.qinghuan.pojo.vo.TicketVerificationInfo;
import com.qinghuan.pojo.vo.VerificationResultVO;
import com.qinghuan.ticket.TicketService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationServiceImplTest {

    @Mock
    private VerificationMapper verificationMapper;
    @Mock
    private TicketService ticketService;

    private VerificationServiceImpl verificationService;

    @BeforeEach
    void setUp() {
        verificationService = new VerificationServiceImpl(verificationMapper, ticketService);
        UserContext.set(new LoginUser(31L, "staff", AccountRole.STAFF, 10L));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void verify_shouldMarkValidTicketUsedAndSaveSuccessRecord() {
        VerificationRequestDTO request = request("REQ-1", "TK-701");
        TicketVerificationInfo ticket = validTicket("TK-701");
        when(ticketService.findForVerification("TK-701", 10L)).thenReturn(ticket);
        when(ticketService.markUsed(any(), any())).thenReturn(1);

        VerificationResultVO result = verificationService.verify(request);

        assertEquals(VerificationResult.SUCCESS, result.getResult());
        assertEquals(TicketStatus.USED, result.getTicket().getStatus());
        assertNotNull(result.getVerifiedAt());
        verify(verificationMapper).insertRecord(any(VerificationRecord.class));
    }

    @Test
    void verify_shouldRecordFailedAttemptWhenTicketWasUsed() {
        TicketVerificationInfo ticket = validTicket("TK-701");
        ticket.setStatus(TicketStatus.USED);
        when(ticketService.findForVerification("TK-701", 10L)).thenReturn(ticket);

        VerificationResultVO result =
                verificationService.verify(request("REQ-2", "TK-701"));

        assertEquals(VerificationResult.FAILED, result.getResult());
        assertEquals("票券已核销", result.getFailureReason());
        verify(ticketService, never()).markUsed(any(), any());
        verify(verificationMapper).insertRecord(any(VerificationRecord.class));
    }

    @Test
    void verify_shouldRecordFailureWhenConditionalUpdateLosesRace() {
        TicketVerificationInfo initial = validTicket("TK-701");
        TicketVerificationInfo latest = validTicket("TK-701");
        latest.setStatus(TicketStatus.USED);
        when(ticketService.findForVerification("TK-701", 10L)).thenReturn(initial);
        when(ticketService.markUsed(any(), any())).thenReturn(0);
        when(ticketService.findForVerification(701L, 10L)).thenReturn(latest);

        VerificationResultVO result =
                verificationService.verify(request("REQ-3", "TK-701"));

        assertEquals(VerificationResult.FAILED, result.getResult());
        assertEquals("票券已核销", result.getFailureReason());
        verify(verificationMapper).updateResult(any(VerificationRecord.class));
    }

    @Test
    void verify_shouldReturnExistingResultForSameRequestNo() {
        VerificationRecord existing = record("REQ-4", 701L, VerificationResult.SUCCESS);
        TicketVerificationInfo ticket = validTicket("TK-701");
        ticket.setStatus(TicketStatus.USED);
        when(verificationMapper.findByRequestNo("REQ-4")).thenReturn(existing);
        when(ticketService.findForVerification(701L, 10L)).thenReturn(ticket);

        VerificationResultVO result =
                verificationService.verify(request("REQ-4", "TK-701"));

        assertEquals(VerificationResult.SUCCESS, result.getResult());
        verify(ticketService, never()).markUsed(any(), any());
        verify(verificationMapper, never()).insertRecord(any());
    }

    @Test
    void verify_shouldRejectRequestNoReusedForAnotherTicket() {
        VerificationRecord existing = record("REQ-5", 701L, VerificationResult.SUCCESS);
        when(verificationMapper.findByRequestNo("REQ-5")).thenReturn(existing);
        when(ticketService.findForVerification(701L, 10L))
                .thenReturn(validTicket("TK-701"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> verificationService.verify(request("REQ-5", "TK-999")));

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
    }

    @Test
    void verify_shouldNotUpdateTicketWhenConcurrentRequestNoBelongsToAnotherTicket() {
        VerificationRecord existing = record("REQ-7", 702L, VerificationResult.SUCCESS);
        when(ticketService.findForVerification("TK-701", 10L))
                .thenReturn(validTicket("TK-701"));
        doThrow(new DuplicateKeyException("duplicate request_no"))
                .when(verificationMapper).insertRecord(any());
        when(verificationMapper.findByRequestNoForUpdate("REQ-7"))
                .thenReturn(existing);
        when(ticketService.findForVerification(702L, 10L))
                .thenReturn(validTicket("TK-702"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> verificationService.verify(request("REQ-7", "TK-701")));

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        verify(ticketService, never()).markUsed(any(), any());
    }

    @Test
    void verify_shouldReturnNotFoundForUnknownOrOtherVenueTicket() {
        when(ticketService.findForVerification("UNKNOWN", 10L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> verificationService.verify(request("REQ-6", "UNKNOWN")));

        assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
        verify(verificationMapper, never()).insertRecord(any());
    }

    private VerificationRequestDTO request(String requestNo, String ticketCode) {
        return new VerificationRequestDTO(requestNo, ticketCode, "GATE-01");
    }

    private TicketVerificationInfo validTicket(String ticketCode) {
        TicketVerificationInfo ticket = new TicketVerificationInfo();
        ticket.setId(701L);
        ticket.setTicketCode(ticketCode);
        ticket.setStatus(TicketStatus.VALID);
        ticket.setValidFrom(LocalDateTime.now().minusMinutes(10));
        ticket.setValidUntil(LocalDateTime.now().plusMinutes(10));
        ticket.setVenueName("海湾科技馆");
        ticket.setVisitorName("张三");
        ticket.setTicketTypeName("成人票");
        return ticket;
    }

    private VerificationRecord record(
            String requestNo, Long ticketId, VerificationResult result) {
        VerificationRecord record = new VerificationRecord();
        record.setRequestNo(requestNo);
        record.setTicketId(ticketId);
        record.setVerifierId(31L);
        record.setResult(result);
        record.setVerifiedAt(LocalDateTime.now());
        return record;
    }
}
