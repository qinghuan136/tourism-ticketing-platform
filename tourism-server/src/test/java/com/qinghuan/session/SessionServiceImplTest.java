package com.qinghuan.session;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qinghuan.auth.context.UserContext;
import com.qinghuan.auth.model.LoginUser;
import com.qinghuan.common.exception.BusinessException;
import com.qinghuan.common.exception.ErrorCode;
import com.qinghuan.pojo.dto.SessionPageQueryDTO;
import com.qinghuan.pojo.dto.SessionTicketTypeConfigDTO;
import com.qinghuan.pojo.dto.SessionWriteDTO;
import com.qinghuan.pojo.entity.AdmissionSession;
import com.qinghuan.pojo.enums.AccountRole;
import com.qinghuan.pojo.enums.AdmissionSessionStatus;
import com.qinghuan.pojo.enums.SessionEvent;
import com.qinghuan.pojo.vo.PageResult;
import com.qinghuan.pojo.vo.SessionStaticSnapshotVO;
import com.qinghuan.pojo.vo.SessionVO;
import com.qinghuan.redis.CacheClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionServiceImplTest {

    @Mock
    private SessionMapper sessionMapper;
    @Mock
    private CacheClient cacheClient;

    private SessionServiceImpl sessionService;

    @BeforeEach
    void setUp() {
        sessionService = new SessionServiceImpl(
                sessionMapper,
                cacheClient,
                Caffeine.<Long, SessionStaticSnapshotVO>newBuilder().build());
        UserContext.set(new LoginUser(1L, "operator", AccountRole.OPERATOR, 10L));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
        PageHelper.clearPage();
    }

    @Test
    void pageSessions_shouldReturnCurrentVenueSessions() {
        SessionPageQueryDTO queryDTO = new SessionPageQueryDTO();
        AdmissionSession session = session(21L, AdmissionSessionStatus.DRAFT);
        Page<AdmissionSession> page = new Page<>(1, 20);
        page.add(session);
        page.setTotal(1);
        when(sessionMapper.list(10L, queryDTO)).thenReturn(page);
        when(sessionMapper.listTicketTypes(21L)).thenReturn(List.of());

        PageResult<SessionVO> result = sessionService.pageSessions(queryDTO);

        assertEquals(1, result.total());
        assertEquals(21L, result.items().get(0).getId());
    }

    @Test
    void createSession_shouldInitializeDraftAndRemainingQuantities() {
        SessionWriteDTO writeDTO = validWriteDTO();
        when(sessionMapper.countEnabledTicketTypes(10L, List.of(1L))).thenReturn(1);
        doAnswer(invocation -> {
            AdmissionSession session = invocation.getArgument(0);
            session.setId(21L);
            return 1;
        }).when(sessionMapper).insertSession(any(AdmissionSession.class));

        Long sessionId = sessionService.createSession(writeDTO);

        assertEquals(21L, sessionId);
        verify(sessionMapper).insertSessionTicketTypes(anyList());
    }

    @Test
    void createSession_shouldRejectAllocationAboveCapacity() {
        SessionWriteDTO writeDTO = validWriteDTO();
        writeDTO.setTotalCapacity(50);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> sessionService.createSession(writeDTO));

        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
        verify(sessionMapper, never()).insertSession(any());
    }

    @Test
    void getSession_shouldReturnDetails() {
        AdmissionSession session = session(21L, AdmissionSessionStatus.DRAFT);
        when(sessionMapper.findById(21L, 10L)).thenReturn(session);
        when(sessionMapper.listTicketTypes(21L)).thenReturn(List.of());

        SessionVO result = sessionService.getSession(21L);

        assertEquals(21L, result.getId());
        assertEquals(AdmissionSessionStatus.DRAFT, result.getStatus());
    }

    @Test
    void getSessionStartTime_shouldNotDependOnCurrentVenue() {
        AdmissionSession session = session(21L, AdmissionSessionStatus.OPEN);
        when(sessionMapper.findByIdForOrder(21L)).thenReturn(session);

        LocalDateTime startTime = sessionService.getSessionStartTime(21L);

        assertEquals(LocalDateTime.of(session.getVisitDate(), session.getStartTime()), startTime);
    }

    @Test
    void updateDraftSession_shouldReplaceTicketTypeConfiguration() {
        SessionWriteDTO writeDTO = validWriteDTO();
        when(sessionMapper.findById(21L, 10L))
                .thenReturn(session(21L, AdmissionSessionStatus.DRAFT));
        when(sessionMapper.countEnabledTicketTypes(10L, List.of(1L))).thenReturn(1);
        when(sessionMapper.updateDraftSession(any(AdmissionSession.class), any(Long.class)))
                .thenReturn(1);

        sessionService.updateDraftSession(21L, writeDTO);

        verify(sessionMapper).deleteSessionTicketTypes(21L);
        verify(sessionMapper).insertSessionTicketTypes(anyList());
    }

    @Test
    void updateDraftSession_shouldRejectOpenSession() {
        when(sessionMapper.findById(21L, 10L))
                .thenReturn(session(21L, AdmissionSessionStatus.OPEN));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> sessionService.updateDraftSession(21L, validWriteDTO()));

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        verify(sessionMapper, never()).updateDraftSession(any(), any());
    }

    @Test
    void handleSessionEvent_shouldPublishDraftSession() {
        AdmissionSession session = session(21L, AdmissionSessionStatus.DRAFT);
        session.setBookingEndAt(LocalDateTime.now().plusHours(1));
        when(sessionMapper.findById(21L, 10L)).thenReturn(session);
        when(sessionMapper.isOpenable(21L, 100)).thenReturn(true);
        when(sessionMapper.updateStatus(
                21L, 10L, AdmissionSessionStatus.DRAFT, AdmissionSessionStatus.OPEN))
                .thenReturn(1);

        sessionService.handleSessionEvent(21L, SessionEvent.PUBLISH);

        verify(sessionMapper).updateStatus(
                21L, 10L, AdmissionSessionStatus.DRAFT, AdmissionSessionStatus.OPEN);
    }

    @Test
    void handleSessionEvent_shouldRejectEventNotSupportedByCurrentStatus() {
        when(sessionMapper.findById(21L, 10L))
                .thenReturn(session(21L, AdmissionSessionStatus.DRAFT));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> sessionService.handleSessionEvent(21L, SessionEvent.CLOSE_BOOKING));

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        verify(sessionMapper, never()).updateStatus(any(), any(), any(), any());
    }

    @Test
    void handleSessionEvent_shouldRejectCancelWhenOrdersAreUnresolved() {
        when(sessionMapper.findById(21L, 10L))
                .thenReturn(session(21L, AdmissionSessionStatus.OPEN));
        when(sessionMapper.countUnresolvedOrders(21L)).thenReturn(1);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> sessionService.handleSessionEvent(21L, SessionEvent.CANCEL));

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        verify(sessionMapper, never()).updateStatus(any(), any(), any(), any());
    }

    @Test
    void handleSessionEvent_shouldRejectEndBeforeSessionFinishes() {
        AdmissionSession session = session(21L, AdmissionSessionStatus.OPEN);
        session.setVisitDate(LocalDate.now().plusDays(1));
        when(sessionMapper.findById(21L, 10L)).thenReturn(session);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> sessionService.handleSessionEvent(21L, SessionEvent.SESSION_ENDED));

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        verify(sessionMapper, never()).updateStatus(any(), any(), any(), any());
    }

    @Test
    void deleteDraftSession_shouldDeleteConfigurationsAndSession() {
        when(sessionMapper.findById(21L, 10L))
                .thenReturn(session(21L, AdmissionSessionStatus.DRAFT));
        when(sessionMapper.countOrders(21L)).thenReturn(0);
        when(sessionMapper.deleteDraftSession(21L, 10L)).thenReturn(1);

        sessionService.deleteDraftSession(21L);

        verify(sessionMapper).deleteSessionTicketTypes(21L);
        verify(sessionMapper).deleteDraftSession(21L, 10L);
    }

    @Test
    void deleteDraftSession_shouldRejectSessionWithOrders() {
        when(sessionMapper.findById(21L, 10L))
                .thenReturn(session(21L, AdmissionSessionStatus.DRAFT));
        when(sessionMapper.countOrders(21L)).thenReturn(1);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> sessionService.deleteDraftSession(21L));

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        verify(sessionMapper, never()).deleteSessionTicketTypes(21L);
    }

    @Test
    void maintainLifecycle_shouldCloseBookingsBeforeEndingSessions() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 1, 12, 0);
        when(sessionMapper.closeExpiredBookings(now)).thenReturn(2);
        when(sessionMapper.endExpiredSessions(now)).thenReturn(1);

        assertEquals(3, sessionService.maintainLifecycle(now));
        verify(sessionMapper).closeExpiredBookings(now);
        verify(sessionMapper).endExpiredSessions(now);
    }

    private SessionWriteDTO validWriteDTO() {
        SessionTicketTypeConfigDTO ticketType = new SessionTicketTypeConfigDTO();
        ticketType.setTicketTypeId(1L);
        ticketType.setSalePrice(new BigDecimal("50.00"));
        ticketType.setAllocatedQuantity(80);

        SessionWriteDTO writeDTO = new SessionWriteDTO();
        writeDTO.setVisitDate(LocalDate.of(2026, 8, 1));
        writeDTO.setStartTime(LocalTime.of(9, 0));
        writeDTO.setEndTime(LocalTime.of(11, 0));
        writeDTO.setBookingStartAt(LocalDateTime.of(2026, 7, 20, 9, 0));
        writeDTO.setBookingEndAt(LocalDateTime.of(2026, 8, 1, 8, 30));
        writeDTO.setTotalCapacity(100);
        writeDTO.setTicketTypes(List.of(ticketType));
        return writeDTO;
    }

    private AdmissionSession session(Long id, AdmissionSessionStatus status) {
        AdmissionSession session = new AdmissionSession();
        session.setId(id);
        session.setVenueId(10L);
        session.setVisitDate(LocalDate.of(2026, 8, 1));
        session.setStartTime(LocalTime.of(9, 0));
        session.setEndTime(LocalTime.of(11, 0));
        session.setBookingStartAt(LocalDateTime.of(2026, 7, 20, 9, 0));
        session.setBookingEndAt(LocalDateTime.of(2026, 8, 1, 8, 30));
        session.setTotalCapacity(100);
        session.setRemainingCapacity(100);
        session.setStatus(status);
        return session;
    }
}
