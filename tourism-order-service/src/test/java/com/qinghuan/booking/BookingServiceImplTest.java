package com.qinghuan.booking;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qinghuan.auth.context.UserContext;
import com.qinghuan.auth.model.LoginUser;
import com.qinghuan.booking.refund.RefundGateway;
import com.qinghuan.booking.refund.RefundGatewayResult;
import com.qinghuan.common.exception.BusinessException;
import com.qinghuan.common.exception.ErrorCode;
import com.qinghuan.common.response.ApiResponse;
import com.qinghuan.pojo.dto.OrderCreateDTO;
import com.qinghuan.pojo.dto.OrderCreateItemRequest;
import com.qinghuan.pojo.dto.OrderPageQueryDTO;
import com.qinghuan.pojo.dto.VenueOrderPageQueryDTO;
import com.qinghuan.pojo.entity.BookingOrder;
import com.qinghuan.pojo.entity.BookingOrderItem;
import com.qinghuan.pojo.entity.Ticket;
import com.qinghuan.pojo.enums.AccountRole;
import com.qinghuan.pojo.enums.BookingOrderStatus;
import com.qinghuan.pojo.enums.TicketStatus;
import com.qinghuan.pojo.remote.venue.BookingContextDTO;
import com.qinghuan.pojo.remote.venue.BookingContextRequest;
import com.qinghuan.pojo.remote.venue.BookingTicketTypeDTO;
import com.qinghuan.pojo.remote.venue.InventoryChangeRequest;
import com.qinghuan.pojo.remote.user.VisitorForOrderDTO;
import com.qinghuan.pojo.remote.user.CurrentUserProfileDTO;
import com.qinghuan.pojo.remote.coupon.CouponOrderTryResponse;
import com.qinghuan.pojo.vo.OrderCreatedVO;
import com.qinghuan.pojo.vo.OrderDetailVO;
import com.qinghuan.pojo.vo.OrderItemVO;
import com.qinghuan.pojo.vo.OrderSummaryVO;
import com.qinghuan.pojo.vo.PageResult;
import com.qinghuan.ticket.TicketService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingMapper bookingMapper;
    @Mock
    private BookingInventoryTccService inventoryTccService;
    @Mock
    private UserClient userClient;
    @Mock
    private VenueClient venueClient;
    @Mock
    private TicketService ticketService;
    @Mock
    private BookingCouponTccService couponTccService;
    @Mock
    private RefundGateway refundGateway;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RLock lock;
    @Mock
    private TransactionTemplate transactionTemplate;

    private BookingServiceImpl bookingService;

    @BeforeEach
    void setUp() {
        bookingService = new BookingServiceImpl(
                bookingMapper, inventoryTccService, userClient, venueClient,
                ticketService, couponTccService,
                refundGateway,
                redissonClient, transactionTemplate);
        UserContext.set(new LoginUser(7L, "tourist", AccountRole.TOURIST, null));
        lenient().when(userClient.getCurrentProfile())
                .thenReturn(ApiResponse.success(new CurrentUserProfileDTO("游客", "13800000000")));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
        PageHelper.clearPage();
    }

    @Test
    void shouldPageOnlyCurrentTouristOrders() {
        OrderPageQueryDTO queryDTO = new OrderPageQueryDTO();
        queryDTO.setStatus(BookingOrderStatus.PAID);
        OrderSummaryVO summary = orderSummary(501L);
        Page<OrderSummaryVO> page = new Page<>(1, 20);
        page.add(summary);
        page.setTotal(1);
        when(bookingMapper.listMyOrders(7L, BookingOrderStatus.PAID)).thenReturn(page);

        PageResult<OrderSummaryVO> result = bookingService.pageMyOrders(queryDTO);

        assertEquals(1, result.total());
        assertEquals(501L, result.items().get(0).getId());
        verify(bookingMapper).listMyOrders(7L, BookingOrderStatus.PAID);
    }

    @Test
    void shouldReturnCurrentTouristOrderDetailWithItems() {
        OrderDetailVO detail = orderDetail(501L);
        OrderItemVO item = new OrderItemVO();
        item.setId(601L);
        when(bookingMapper.findMyOrderDetail(501L, 7L)).thenReturn(detail);
        when(bookingMapper.listOrderItems(501L)).thenReturn(List.of(item));

        OrderDetailVO result = bookingService.getMyOrder(501L);

        assertEquals(501L, result.getId());
        assertEquals(601L, result.getItems().get(0).getId());
    }

    @Test
    void shouldPageOnlyCurrentVenueOrders() {
        UserContext.set(new LoginUser(8L, "operator", AccountRole.OPERATOR, 10L));
        VenueOrderPageQueryDTO queryDTO = new VenueOrderPageQueryDTO();
        queryDTO.setOrderNo("ORDER-501");
        queryDTO.setSessionId(21L);
        queryDTO.setVisitDate(LocalDate.of(2026, 8, 10));
        OrderSummaryVO summary = orderSummary(501L);
        Page<OrderSummaryVO> page = new Page<>(1, 20);
        page.add(summary);
        page.setTotal(1);
        when(bookingMapper.listVenueOrders(10L, queryDTO)).thenReturn(page);

        PageResult<OrderSummaryVO> result = bookingService.pageVenueOrders(queryDTO);

        assertEquals(1, result.total());
        verify(bookingMapper).listVenueOrders(10L, queryDTO);
    }

    @Test
    void shouldReturnCurrentVenueOrderDetailWithPurchaser() {
        UserContext.set(new LoginUser(9L, "staff", AccountRole.STAFF, 10L));
        OrderDetailVO detail = orderDetail(501L);
        detail.setPurchaserUserId(7L);
        detail.setPurchaserName("游客甲");
        when(bookingMapper.findVenueOrderDetail(501L, 10L)).thenReturn(detail);
        when(bookingMapper.listOrderItems(501L)).thenReturn(List.of());

        OrderDetailVO result = bookingService.getVenueOrder(501L);

        assertEquals(7L, result.getPurchaserUserId());
        assertEquals("游客甲", result.getPurchaserName());
    }

    @Test
    void shouldHideOrderOutsideCurrentVenueAsNotFound() {
        UserContext.set(new LoginUser(8L, "operator", AccountRole.OPERATOR, 10L));
        when(bookingMapper.findVenueOrderDetail(501L, 10L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class, () -> bookingService.getVenueOrder(501L));

        assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
        verify(bookingMapper, never()).listOrderItems(501L);
    }

    @Test
    void shouldMaskVisitorIdNumberInOrderItemResponse() {
        OrderItemVO item = new OrderItemVO();

        item.setVisitorIdNumber("440101199001011234");

        assertEquals("440***********1234", item.getMaskedVisitorIdNumber());
    }

    @Test
    void shouldCreateOrderWithAmountSnapshotsAndInventoryReservation() {
        allowOrderCreation();
        VisitorForOrderDTO visitor1 = visitor(101L, "张三", "ID_CARD", "440101199001011234");
        VisitorForOrderDTO visitor2 = visitor(102L, "李四", "PASSPORT", "P1234567");
        whenActiveVisitors(visitor1, visitor2);
        when(venueClient.getBookingContext(any()))
                .thenReturn(ApiResponse.success(bookingContext(
                        ticketType(301L, "成人票", "80.00"),
                        ticketType(302L, "学生票", "40.00"))));
        OrderCreatedVO result = bookingService.createOrder(new OrderCreateDTO(
                21L,
                List.of(
                        new OrderCreateItemRequest(101L, 301L),
                        new OrderCreateItemRequest(102L, 302L)), null));

        assertNotNull(result.id());
        assertEquals(new BigDecimal("120.00"), result.totalAmount());
        assertEquals(BookingOrderStatus.PENDING_PAYMENT, result.status());
        assertNotNull(result.expireAt());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BookingOrderItem>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(bookingMapper).insertOrderItems(itemsCaptor.capture());
        List<BookingOrderItem> savedItems = itemsCaptor.getValue();
        assertEquals(2, savedItems.size());
        assertEquals(result.id(), savedItems.get(0).getOrderId());
        assertEquals("张三", savedItems.get(0).getVisitorName());
        assertEquals("成人票", savedItems.get(0).getTicketTypeName());
        assertEquals(new BigDecimal("80.00"), savedItems.get(0).getUnitPrice());
        verify(inventoryTccService).tryReserve(
                result.id(), 21L, Map.of(301L, 1, 302L, 1));
        verify(inventoryTccService).confirm(result.id());
    }

    @Test
    void shouldLockCouponAndSaveDiscountedOrderAmount() {
        allowOrderCreation();
        VisitorForOrderDTO visitor = visitor(101L, "张三", "ID_CARD", "440101199001011234");
        whenActiveVisitors(visitor);
        when(venueClient.getBookingContext(any())).thenReturn(
                ApiResponse.success(bookingContext(ticketType(301L, "成人票", "120.00"))));
        when(couponTccService.tryLock(any()))
                .thenReturn(new CouponOrderTryResponse(
                        2001L, new BigDecimal("20.00"), new BigDecimal("100.00")));
        bookingService.createOrder(new OrderCreateDTO(
                21L, List.of(new OrderCreateItemRequest(101L, 301L)), 2001L));

        ArgumentCaptor<BookingOrder> orderCaptor = ArgumentCaptor.forClass(BookingOrder.class);
        verify(bookingMapper).insertOrder(orderCaptor.capture());
        BookingOrder saved = orderCaptor.getValue();
        assertEquals(new BigDecimal("120.00"), saved.getOriginalAmount());
        assertEquals(new BigDecimal("20.00"), saved.getDiscountAmount());
        assertEquals(new BigDecimal("100.00"), saved.getTotalAmount());
        assertEquals(2001L, saved.getUserCouponId());
    }

    @Test
    void shouldRejectSameVisitorEvenWhenTicketTypesDiffer() {
        OrderCreateDTO request = new OrderCreateDTO(
                21L,
                List.of(
                        new OrderCreateItemRequest(101L, 301L),
                        new OrderCreateItemRequest(101L, 302L)), null);

        BusinessException exception = assertThrows(
                BusinessException.class, () -> bookingService.createOrder(request));

        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
        verify(bookingMapper, never()).insertOrder(any());
    }

    @Test
    void shouldMarkFreeOrderPaidAndCreateTickets() {
        allowOrderCreation();
        VisitorForOrderDTO visitor = visitor(101L, "张三", "ID_CARD", "440101199001011234");
        whenActiveVisitors(visitor);
        when(venueClient.getBookingContext(any())).thenReturn(
                ApiResponse.success(bookingContext(ticketType(301L, "免费票", "0.00"))));
        OrderCreatedVO result = bookingService.createOrder(new OrderCreateDTO(
                21L, List.of(new OrderCreateItemRequest(101L, 301L)), null));

        assertEquals(BookingOrderStatus.PAID, result.status());
        assertNull(result.expireAt());
        verify(ticketService).createTicketsForOrder(result.id(), 1);
    }

    @Test
    void shouldRejectVisitorWhoAlreadyHasActiveOrderInSession() {
        allowOrderCreation();
        VisitorForOrderDTO visitor = visitor(101L, "张三", "ID_CARD", "440101199001011234");
        OrderCreateItemRequest item = new OrderCreateItemRequest(101L, 302L);
        whenActiveVisitors(visitor);
        when(bookingMapper.findConflictingOrdersBySessionAndFingerprints(
                21L, List.of("fingerprint-101")))
                .thenReturn(List.of(refundOrder(BookingOrderStatus.PAID)));
        when(venueClient.getBookingContext(any())).thenReturn(
                ApiResponse.success(bookingContext(ticketType(302L, "学生票", "40.00"))));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> bookingService.createOrder(new OrderCreateDTO(
                        21L, List.of(item), null)));

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        verify(bookingMapper, never()).insertOrder(any());
        verify(redissonClient).getLock("lock:booking:21:fingerprint-101");
        verify(lock).unlock();
        verify(inventoryTccService).markCanceling(any());
        verify(inventoryTccService).cancel(any());
    }

    @Test
    void shouldRejectDifferentVisitorIdsWithSameFingerprint() {
        VisitorForOrderDTO first = visitor(101L, "张三", "ID_CARD", "440101199001011234");
        VisitorForOrderDTO second = new VisitorForOrderDTO(
                102L, "张三", "ID_CARD", "440101199001011234", first.fingerprint());
        whenActiveVisitors(first, second);

        BusinessException exception = assertThrows(BusinessException.class, () -> bookingService.createOrder(
                new OrderCreateDTO(21L, List.of(
                        new OrderCreateItemRequest(101L, 301L),
                        new OrderCreateItemRequest(102L, 302L)), null)));

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        verify(redissonClient, never()).getLock(anyString());
    }

    @Test
    void shouldAcquireBookingLocksInFingerprintOrder() {
        allowOrderCreation();
        VisitorForOrderDTO first = visitor(101L, "张三", "ID_CARD", "440101199001011234");
        VisitorForOrderDTO second = visitor(102L, "李四", "PASSPORT", "P1234567");
        whenActiveVisitors(first, second);
        when(bookingMapper.findConflictingOrdersBySessionAndFingerprints(
                21L, List.of("fingerprint-101", "fingerprint-102")))
                .thenReturn(List.of(refundOrder(BookingOrderStatus.PAID)));
        when(venueClient.getBookingContext(any())).thenReturn(ApiResponse.success(bookingContext(
                ticketType(301L, "成人票", "80.00"),
                ticketType(302L, "学生票", "40.00"))));

        assertThrows(BusinessException.class, () -> bookingService.createOrder(new OrderCreateDTO(
                21L, List.of(
                        new OrderCreateItemRequest(102L, 302L),
                        new OrderCreateItemRequest(101L, 301L)), null)));

        InOrder order = inOrder(redissonClient);
        order.verify(redissonClient).getLock("lock:booking:21:fingerprint-101");
        order.verify(redissonClient).getLock("lock:booking:21:fingerprint-102");
    }

    @Test
    void shouldReleasePreviouslyAcquiredLocksWhenLaterLockFails() {
        VisitorForOrderDTO first = visitor(101L, "张三", "ID_CARD", "440101199001011234");
        VisitorForOrderDTO second = visitor(102L, "李四", "PASSPORT", "P1234567");
        RLock firstLock = mock(RLock.class);
        RLock secondLock = mock(RLock.class);
        whenActiveVisitors(first, second);
        when(redissonClient.getLock("lock:booking:21:fingerprint-101")).thenReturn(firstLock);
        when(redissonClient.getLock("lock:booking:21:fingerprint-102")).thenReturn(secondLock);
        when(firstLock.tryLock()).thenReturn(true);
        when(firstLock.isHeldByCurrentThread()).thenReturn(true);
        when(secondLock.tryLock()).thenReturn(false);

        assertThrows(BusinessException.class, () -> bookingService.createOrder(new OrderCreateDTO(
                21L, List.of(
                        new OrderCreateItemRequest(102L, 302L),
                        new OrderCreateItemRequest(101L, 301L)), null)));

        verify(firstLock).unlock();
        verify(secondLock, never()).unlock();
    }

    @Test
    void shouldPayPendingOrderAndCreateTickets() {
        BookingOrder order = refundOrder(BookingOrderStatus.PENDING_PAYMENT);
        order.setQuantity(2);
        order.setExpireAt(LocalDateTime.now().plusMinutes(10));
        when(bookingMapper.findOrderByOrderId(501L)).thenReturn(order);
        when(bookingMapper.updatePaidOrder(order, BookingOrderStatus.PENDING_PAYMENT))
                .thenReturn(1);

        boolean paid = bookingService.payOrder(501L);

        assertTrue(paid);
        assertEquals(BookingOrderStatus.PAID, order.getStatus());
        assertNotNull(order.getPaymentNo());
        assertNotNull(order.getPaidAt());
        verify(ticketService).createTicketsForOrder(501L, 2);
        verify(venueClient, never()).releaseInventory(any());
    }

    @Test
    void shouldCloseExpiredOrderAndRestoreInventoryWhenPaying() {
        BookingOrder order = refundOrder(BookingOrderStatus.PENDING_PAYMENT);
        order.setExpireAt(LocalDateTime.now().minusMinutes(1));
        OrderItemVO item = new OrderItemVO();
        item.setSessionTicketTypeId(301L);
        when(bookingMapper.findOrderByOrderId(501L)).thenReturn(order);
        when(bookingMapper.updateOrder(order, BookingOrderStatus.PENDING_PAYMENT)).thenReturn(1);
        when(bookingMapper.listOrderItems(501L)).thenReturn(List.of(item));

        boolean paid = bookingService.payOrder(501L);

        assertFalse(paid);
        assertEquals(BookingOrderStatus.CLOSED, order.getStatus());
        assertNotNull(order.getClosedAt());
        verify(venueClient).releaseInventory(new InventoryChangeRequest(21L, Map.of(301L, 1)));
        verify(ticketService, never()).createTicketsForOrder(any(), anyInt());
    }

    @Test
    void shouldRejectPayWhenOrderIsNotPendingPayment() {
        when(bookingMapper.findOrderByOrderId(501L))
                .thenReturn(refundOrder(BookingOrderStatus.PAID));

        BusinessException exception = assertThrows(
                BusinessException.class, () -> bookingService.payOrder(501L));

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        verify(bookingMapper, never()).updatePaidOrder(any(), any());
        verify(ticketService, never()).createTicketsForOrder(any(), anyInt());
    }

    @Test
    void shouldRejectPayWhenConditionalUpdateLosesConcurrencyRace() {
        BookingOrder order = refundOrder(BookingOrderStatus.PENDING_PAYMENT);
        order.setQuantity(1);
        order.setExpireAt(LocalDateTime.now().plusMinutes(10));
        when(bookingMapper.findOrderByOrderId(501L)).thenReturn(order);
        when(bookingMapper.updatePaidOrder(order, BookingOrderStatus.PENDING_PAYMENT))
                .thenReturn(0);

        BusinessException exception = assertThrows(
                BusinessException.class, () -> bookingService.payOrder(501L));

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        verify(ticketService, never()).createTicketsForOrder(any(), anyInt());
    }

    @Test
    void shouldRejectVisitorNotOwnedByCurrentTourist() {
        whenActiveVisitors(visitor(101L, "张三", "ID_CARD", "440101199001011234"));
        OrderCreateDTO request = new OrderCreateDTO(
                21L, List.of(new OrderCreateItemRequest(999L, 301L)), null);

        BusinessException exception = assertThrows(
                BusinessException.class, () -> bookingService.createOrder(request));

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        verify(bookingMapper, never()).insertOrder(any());
    }

    @Test
    void shouldRefundPaidOrderAndRestoreTicketsAndInventory() {
        runTransactions();
        BookingOrder order = refundOrder(BookingOrderStatus.PAID);
        Ticket ticket = new Ticket();
        ticket.setOrderItemId(601L);
        ticket.setStatus(TicketStatus.VALID);
        OrderItemVO item = new OrderItemVO();
        item.setSessionTicketTypeId(301L);

        when(bookingMapper.findOrderByOrderId(501L)).thenReturn(order);
        when(venueClient.getBookingContext(new BookingContextRequest(21L, List.of())))
                .thenReturn(ApiResponse.success(bookingContext()));
        when(ticketService.listTicketsByOrderId(501L)).thenReturn(List.of(ticket));
        when(bookingMapper.listOrderItems(501L)).thenReturn(List.of(item));
        when(bookingMapper.updateOrder(order, BookingOrderStatus.PAID)).thenReturn(1);
        when(ticketService.markRefunding(501L)).thenAnswer(invocation -> {
            ticket.setStatus(TicketStatus.REFUNDING);
            return 1;
        });
        when(refundGateway.requestRefund(any())).thenReturn(RefundGatewayResult.SUCCESS);
        when(bookingMapper.updateOrder(order, BookingOrderStatus.REFUNDING)).thenReturn(1);
        when(ticketService.completeRefund(501L)).thenAnswer(invocation -> {
            ticket.setStatus(TicketStatus.VOID);
            return 1;
        });

        bookingService.refundOrder(501L);

        assertEquals(BookingOrderStatus.REFUNDED, order.getStatus());
        assertNotNull(order.getRefundAt());
        assertEquals(TicketStatus.VOID, ticket.getStatus());
        verify(venueClient).releaseInventory(new InventoryChangeRequest(21L, Map.of(301L, 1)));
    }

    @Test
    void shouldRejectRefundWhenTicketWasUsed() {
        runTransactions();
        Ticket ticket = new Ticket();
        ticket.setStatus(TicketStatus.USED);
        when(bookingMapper.findOrderByOrderId(501L))
                .thenReturn(refundOrder(BookingOrderStatus.PAID));
        when(venueClient.getBookingContext(new BookingContextRequest(21L, List.of())))
                .thenReturn(ApiResponse.success(bookingContext()));
        when(ticketService.listTicketsByOrderId(501L)).thenReturn(List.of(ticket));

        BusinessException exception = assertThrows(
                BusinessException.class, () -> bookingService.refundOrder(501L));

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        verify(bookingMapper, never()).updateOrder(any(), any());
        verify(venueClient, never()).releaseInventory(any());
    }

    @Test
    void shouldKeepOrderRefundingWhenGatewayResultIsUnknown() {
        runTransactions();
        BookingOrder order = refundOrder(BookingOrderStatus.PAID);
        Ticket ticket = new Ticket();
        ticket.setOrderItemId(601L);
        ticket.setStatus(TicketStatus.VALID);

        when(bookingMapper.findOrderByOrderId(501L)).thenReturn(order);
        when(venueClient.getBookingContext(new BookingContextRequest(21L, List.of())))
                .thenReturn(ApiResponse.success(bookingContext()));
        when(ticketService.listTicketsByOrderId(501L)).thenReturn(List.of(ticket));
        when(bookingMapper.updateOrder(order, BookingOrderStatus.PAID)).thenReturn(1);
        when(ticketService.markRefunding(501L)).thenAnswer(invocation -> {
            ticket.setStatus(TicketStatus.REFUNDING);
            return 1;
        });
        when(refundGateway.requestRefund(any())).thenReturn(RefundGatewayResult.UNKNOWN);

        bookingService.refundOrder(501L);

        assertEquals(BookingOrderStatus.REFUNDING, order.getStatus());
        assertNotNull(order.getRefundNo());
        assertEquals(TicketStatus.REFUNDING, ticket.getStatus());
        verify(venueClient, never()).releaseInventory(any());
    }

    @Test
    void shouldRejectRefundWhenOrderIsNotPaid() {
        runTransactions();
        when(bookingMapper.findOrderByOrderId(501L))
                .thenReturn(refundOrder(BookingOrderStatus.PENDING_PAYMENT));

        BusinessException exception = assertThrows(
                BusinessException.class, () -> bookingService.refundOrder(501L));

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        verify(venueClient, never()).getBookingContext(any());
    }

    @Test
    void shouldReturnNotFoundWhenRefundOrderDoesNotExist() {
        runTransactions();
        when(bookingMapper.findOrderByOrderId(501L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class, () -> bookingService.refundOrder(501L));

        assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void shouldCancelPendingOrderAndRestoreInventory() {
        BookingOrder order = refundOrder(BookingOrderStatus.PENDING_PAYMENT);
        OrderItemVO item = new OrderItemVO();
        item.setSessionTicketTypeId(301L);
        when(bookingMapper.findOrderByOrderId(501L)).thenReturn(order);
        when(bookingMapper.updateOrder(order, BookingOrderStatus.PENDING_PAYMENT)).thenReturn(1);
        when(bookingMapper.listOrderItems(501L)).thenReturn(List.of(item));

        bookingService.cancelOrder(501L);

        assertEquals(BookingOrderStatus.CANCELLED, order.getStatus());
        assertNotNull(order.getCancelledAt());
        verify(venueClient).releaseInventory(new InventoryChangeRequest(21L, Map.of(301L, 1)));
    }

    @Test
    void shouldReturnNotFoundWhenCancelOrderDoesNotExist() {
        when(bookingMapper.findOrderByOrderId(501L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class, () -> bookingService.cancelOrder(501L));

        assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void shouldRejectCancelWhenOrderIsNotPendingPayment() {
        when(bookingMapper.findOrderByOrderId(501L))
                .thenReturn(refundOrder(BookingOrderStatus.PAID));

        BusinessException exception = assertThrows(
                BusinessException.class, () -> bookingService.cancelOrder(501L));

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        verify(bookingMapper, never()).updateOrder(any(), any());
    }

    @Test
    void shouldRecordClosedAtWhenClosingTimeoutOrder() {
        BookingOrder order = refundOrder(BookingOrderStatus.PENDING_PAYMENT);
        OrderItemVO item = new OrderItemVO();
        item.setSessionTicketTypeId(301L);
        when(bookingMapper.findOrderByOrderId(501L)).thenReturn(order);
        when(bookingMapper.updateOrder(order, BookingOrderStatus.PENDING_PAYMENT)).thenReturn(1);
        when(bookingMapper.listOrderItems(501L)).thenReturn(List.of(item));

        bookingService.cancelTimeoutOrder(501L);

        assertEquals(BookingOrderStatus.CLOSED, order.getStatus());
        assertNotNull(order.getClosedAt());
        assertNull(order.getCancelledAt());
        verify(venueClient).releaseInventory(new InventoryChangeRequest(21L, Map.of(301L, 1)));
    }

    private VisitorForOrderDTO visitor(Long id, String name, String idType, String idNumber) {
        return new VisitorForOrderDTO(id, name, idType, idNumber, "fingerprint-" + id);
    }

    private void whenActiveVisitors(VisitorForOrderDTO... visitors) {
        when(userClient.listActiveVisitorsForOrder())
                .thenReturn(ApiResponse.success(List.of(visitors)));
    }

    private void allowOrderCreation() {
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock()).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        runTransactions();
    }

    private void runTransactions() {
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
    }

    private BookingTicketTypeDTO ticketType(Long id, String name, String price) {
        return new BookingTicketTypeDTO(id, name, new BigDecimal(price));
    }

    private BookingContextDTO bookingContext(BookingTicketTypeDTO... ticketTypes) {
        return new BookingContextDTO(
                21L, 10L, "景点甲", "测试路 1 号",
                LocalDate.now().plusDays(1), java.time.LocalTime.of(9, 0), java.time.LocalTime.of(11, 0),
                LocalDateTime.now().plusDays(1), List.of(ticketTypes));
    }

    private BookingOrder refundOrder(BookingOrderStatus status) {
        BookingOrder order = new BookingOrder();
        order.setId(501L);
        order.setUserId(7L);
        order.setSessionId(21L);
        order.setStatus(status);
        return order;
    }

    private OrderSummaryVO orderSummary(Long id) {
        OrderSummaryVO summary = new OrderSummaryVO();
        summary.setId(id);
        summary.setOrderNo("ORDER-" + id);
        summary.setStatus(BookingOrderStatus.PAID);
        return summary;
    }

    private OrderDetailVO orderDetail(Long id) {
        OrderDetailVO detail = new OrderDetailVO();
        detail.setId(id);
        detail.setOrderNo("ORDER-" + id);
        detail.setStatus(BookingOrderStatus.PAID);
        return detail;
    }
}
