package com.qinghuan.booking;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qinghuan.auth.context.UserContext;
import com.qinghuan.auth.model.LoginUser;
import com.qinghuan.booking.refund.RefundGateway;
import com.qinghuan.booking.refund.RefundGatewayResult;
import com.qinghuan.common.exception.BusinessException;
import com.qinghuan.common.exception.ErrorCode;
import com.qinghuan.coupon.CouponOrderService;
import com.qinghuan.coupon.CouponDiscount;
import com.qinghuan.pojo.dto.OrderCreateDTO;
import com.qinghuan.pojo.dto.OrderCreateItemRequest;
import com.qinghuan.pojo.dto.OrderPageQueryDTO;
import com.qinghuan.pojo.dto.VenueOrderPageQueryDTO;
import com.qinghuan.pojo.entity.BookingOrder;
import com.qinghuan.pojo.entity.BookingOrderItem;
import com.qinghuan.pojo.entity.Ticket;
import com.qinghuan.pojo.entity.Visitor;
import com.qinghuan.pojo.enums.AccountRole;
import com.qinghuan.pojo.enums.BookingOrderStatus;
import com.qinghuan.pojo.enums.TicketStatus;
import com.qinghuan.pojo.vo.OrderCreatedVO;
import com.qinghuan.pojo.vo.OrderDetailVO;
import com.qinghuan.pojo.vo.OrderItemVO;
import com.qinghuan.pojo.vo.OrderSummaryVO;
import com.qinghuan.pojo.vo.PageResult;
import com.qinghuan.pojo.vo.SessionTicketTypeVO;
import com.qinghuan.session.SessionInventoryService;
import com.qinghuan.session.SessionService;
import com.qinghuan.ticket.TicketService;
import com.qinghuan.visitor.VisitorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingMapper bookingMapper;
    @Mock
    private VisitorService visitorService;
    @Mock
    private SessionInventoryService sessionInventoryService;
    @Mock
    private TicketService ticketService;
    @Mock
    private SessionService sessionService;
    @Mock
    private CouponOrderService couponOrderService;
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
                bookingMapper, visitorService, sessionInventoryService,
                ticketService, sessionService, couponOrderService,
                refundGateway,
                redissonClient, transactionTemplate);
        UserContext.set(new LoginUser(7L, "tourist", AccountRole.TOURIST, null));
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
        Visitor visitor1 = visitor(101L, "张三", "ID_CARD", "440101199001011234");
        Visitor visitor2 = visitor(102L, "李四", "PASSPORT", "P1234567");
        when(visitorService.listActiveVisitorsForOrder())
                .thenReturn(List.of(visitor1, visitor2));
        when(sessionInventoryService.getOrderableTicketTypes(eq(21L), anyList()))
                .thenReturn(List.of(
                        ticketType(301L, "成人票", "80.00"),
                        ticketType(302L, "学生票", "40.00")));
        when(bookingMapper.insertOrder(any(BookingOrder.class))).thenAnswer(invocation -> {
            BookingOrder order = invocation.getArgument(0);
            order.setId(9001L);
            return 1;
        });

        OrderCreatedVO result = bookingService.createOrder(new OrderCreateDTO(
                21L,
                List.of(
                        new OrderCreateItemRequest(101L, 301L),
                        new OrderCreateItemRequest(102L, 302L)), null));

        assertEquals(9001L, result.id());
        assertEquals(new BigDecimal("120.00"), result.totalAmount());
        assertEquals(BookingOrderStatus.PENDING_PAYMENT, result.status());
        assertNotNull(result.expireAt());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BookingOrderItem>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(bookingMapper).insertOrderItems(itemsCaptor.capture());
        List<BookingOrderItem> savedItems = itemsCaptor.getValue();
        assertEquals(2, savedItems.size());
        assertEquals(9001L, savedItems.get(0).getOrderId());
        assertEquals("张三", savedItems.get(0).getVisitorName());
        assertEquals("成人票", savedItems.get(0).getTicketTypeName());
        assertEquals(new BigDecimal("80.00"), savedItems.get(0).getUnitPrice());
        verify(sessionInventoryService).reserveInventory(
                21L, Map.of(301L, 1, 302L, 1));
    }

    @Test
    void shouldLockCouponAndSaveDiscountedOrderAmount() {
        allowOrderCreation();
        Visitor visitor = visitor(101L, "张三", "ID_CARD", "440101199001011234");
        when(visitorService.listActiveVisitorsForOrder()).thenReturn(List.of(visitor));
        when(sessionInventoryService.getOrderableTicketTypes(eq(21L), anyList()))
                .thenReturn(List.of(ticketType(301L, "成人票", "120.00")));
        when(sessionService.getSessionVenueId(21L)).thenReturn(10L);
        when(couponOrderService.lockForOrder(
                2001L, 7L, 10L, new BigDecimal("120.00")))
                .thenReturn(new CouponDiscount(
                        2001L, new BigDecimal("20.00"), new BigDecimal("100.00")));
        when(bookingMapper.insertOrder(any(BookingOrder.class))).thenAnswer(invocation -> {
            BookingOrder order = invocation.getArgument(0);
            order.setId(9003L);
            return 1;
        });

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
        Visitor visitor = visitor(101L, "张三", "ID_CARD", "440101199001011234");
        when(visitorService.listActiveVisitorsForOrder()).thenReturn(List.of(visitor));
        when(sessionInventoryService.getOrderableTicketTypes(eq(21L), anyList()))
                .thenReturn(List.of(ticketType(301L, "免费票", "0.00")));
        when(bookingMapper.insertOrder(any(BookingOrder.class))).thenAnswer(invocation -> {
            BookingOrder order = invocation.getArgument(0);
            order.setId(9002L);
            return 1;
        });

        OrderCreatedVO result = bookingService.createOrder(new OrderCreateDTO(
                21L, List.of(new OrderCreateItemRequest(101L, 301L)), null));

        assertEquals(BookingOrderStatus.PAID, result.status());
        assertNull(result.expireAt());
        verify(ticketService).createTicketsForOrder(9002L, 1);
    }

    @Test
    void shouldRejectVisitorWhoAlreadyHasActiveOrderInSession() {
        allowOrderCreation();
        Visitor visitor = visitor(101L, "张三", "ID_CARD", "440101199001011234");
        OrderCreateItemRequest item = new OrderCreateItemRequest(101L, 302L);
        when(visitorService.listActiveVisitorsForOrder()).thenReturn(List.of(visitor));
        when(bookingMapper.findConflictingOrdersBySessionAndVisitorIds(
                21L, List.of(101L)))
                .thenReturn(List.of(refundOrder(BookingOrderStatus.PAID)));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> bookingService.createOrder(new OrderCreateDTO(
                        21L, List.of(item), null)));

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        verify(bookingMapper, never()).insertOrder(any());
        verify(redissonClient).getLock("lock:booking:21:101");
        verify(lock).unlock();
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
        verify(sessionInventoryService, never()).releaseInventory(any(), any());
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
        verify(sessionInventoryService).releaseInventory(21L, Map.of(301L, 1));
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
        when(visitorService.listActiveVisitorsForOrder())
                .thenReturn(List.of(visitor(101L, "张三", "ID_CARD", "440101199001011234")));
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
        when(sessionService.getSessionStartTime(21L))
                .thenReturn(LocalDateTime.now().plusDays(1));
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
        verify(sessionInventoryService).releaseInventory(21L, Map.of(301L, 1));
    }

    @Test
    void shouldRejectRefundWhenTicketWasUsed() {
        runTransactions();
        Ticket ticket = new Ticket();
        ticket.setStatus(TicketStatus.USED);
        when(bookingMapper.findOrderByOrderId(501L))
                .thenReturn(refundOrder(BookingOrderStatus.PAID));
        when(sessionService.getSessionStartTime(21L))
                .thenReturn(LocalDateTime.now().plusDays(1));
        when(ticketService.listTicketsByOrderId(501L)).thenReturn(List.of(ticket));

        BusinessException exception = assertThrows(
                BusinessException.class, () -> bookingService.refundOrder(501L));

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        verify(bookingMapper, never()).updateOrder(any(), any());
        verify(sessionInventoryService, never()).releaseInventory(any(), any());
    }

    @Test
    void shouldKeepOrderRefundingWhenGatewayResultIsUnknown() {
        runTransactions();
        BookingOrder order = refundOrder(BookingOrderStatus.PAID);
        Ticket ticket = new Ticket();
        ticket.setOrderItemId(601L);
        ticket.setStatus(TicketStatus.VALID);

        when(bookingMapper.findOrderByOrderId(501L)).thenReturn(order);
        when(sessionService.getSessionStartTime(21L))
                .thenReturn(LocalDateTime.now().plusDays(1));
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
        verify(sessionInventoryService, never()).releaseInventory(any(), any());
    }

    @Test
    void shouldRejectRefundWhenOrderIsNotPaid() {
        runTransactions();
        when(bookingMapper.findOrderByOrderId(501L))
                .thenReturn(refundOrder(BookingOrderStatus.PENDING_PAYMENT));

        BusinessException exception = assertThrows(
                BusinessException.class, () -> bookingService.refundOrder(501L));

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        verify(sessionService, never()).getSessionStartTime(any());
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
        verify(sessionInventoryService).releaseInventory(21L, Map.of(301L, 1));
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
        verify(sessionInventoryService).releaseInventory(21L, Map.of(301L, 1));
    }

    private Visitor visitor(Long id, String name, String idType, String idNumber) {
        Visitor visitor = new Visitor();
        visitor.setId(id);
        visitor.setName(name);
        visitor.setIdType(idType);
        visitor.setIdNumber(idNumber);
        return visitor;
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

    private SessionTicketTypeVO ticketType(Long id, String name, String price) {
        SessionTicketTypeVO ticketType = new SessionTicketTypeVO();
        ticketType.setSessionTicketTypeId(id);
        ticketType.setTicketTypeName(name);
        ticketType.setSalePrice(new BigDecimal(price));
        return ticketType;
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
