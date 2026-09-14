package com.qinghuan.booking;

import com.qinghuan.pojo.dto.VenueOrderPageQueryDTO;
import com.qinghuan.pojo.entity.BookingOrder;
import com.qinghuan.pojo.entity.Ticket;
import com.qinghuan.pojo.enums.BookingOrderStatus;
import com.qinghuan.pojo.enums.TicketStatus;
import com.qinghuan.pojo.vo.OrderDetailVO;
import com.qinghuan.pojo.vo.OrderItemVO;
import com.qinghuan.pojo.vo.OrderSummaryVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import com.qinghuan.ticket.TicketMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@MybatisTest(properties = {
        "mybatis.mapper-locations=classpath:mapper/**/*.xml",
        "mybatis.configuration.map-underscore-to-camel-case=true"
})
@Sql(statements = {
        "drop table if exists ticket",
        "drop table if exists booking_order_item",
        "drop table if exists booking_order",
        "drop table if exists visitor_identity",
        "drop table if exists visitor",
        "drop table if exists admission_session",
        "drop table if exists venue",
        "drop table if exists user_account",
        """
        create table venue (
            id bigint primary key,
            name varchar(100) not null
        );
        """,
        """
        create table user_account (
            id bigint primary key,
            display_name varchar(50),
            phone varchar(20)
        );
        """,
        """
        create table admission_session (
            id bigint primary key,
            venue_id bigint not null,
            visit_date date not null,
            start_time time not null,
            end_time time not null
        );
        """,
        """
        create table booking_order (
            id bigint primary key,
            order_no varchar(32) not null,
            user_id bigint not null,
            session_id bigint not null,
            quantity int not null,
            user_coupon_id bigint,
            original_amount decimal(10, 2) not null,
            discount_amount decimal(10, 2) not null default 0,
            total_amount decimal(10, 2) not null,
            status varchar(30) not null,
            payment_no varchar(64),
            refund_no varchar(64),
            expire_at timestamp,
            paid_at timestamp,
            cancelled_at timestamp,
            closed_at timestamp,
            completed_at timestamp,
            refund_requested_at timestamp,
            refund_at timestamp,
            created_at timestamp not null
        );
        """,
        """
        create table booking_order_item (
            id bigint primary key,
            order_id bigint not null,
            visitor_id bigint not null,
            session_ticket_type_id bigint not null,
            visitor_name varchar(50) not null,
            visitor_id_type varchar(20) not null,
            visitor_id_number varchar(64) not null,
            ticket_type_name varchar(100) not null,
            unit_price decimal(10, 2) not null
        );
        """,
        """
        create table visitor (
            id bigint primary key,
            user_id bigint not null,
            name varchar(50) not null,
            id_type varchar(20) not null,
            id_number varchar(64) not null,
            status varchar(20) not null
        );
        """,
        """
        create table visitor_identity (
            visitor_id bigint primary key,
            fingerprint char(64) not null
        );
        """,
        """
        create table ticket (
            id bigint primary key,
            ticket_code varchar(64) not null,
            order_item_id bigint not null,
            status varchar(20) not null,
            valid_from timestamp not null,
            valid_until timestamp not null,
            verified_at timestamp,
            created_at timestamp default current_timestamp,
            updated_at timestamp default current_timestamp
        );
        """,
        "insert into venue(id, name) values (10, '景点甲'), (20, '景点乙')",
        "insert into user_account(id, display_name, phone) values (7, '游客甲', '13800000001'), (8, '游客乙', '13800000002')",
        """
        insert into admission_session(id, venue_id, visit_date, start_time, end_time)
        values (21, 10, '2026-08-10', '09:00:00', '11:00:00'),
               (22, 20, '2026-08-11', '14:00:00', '16:00:00');
        """,
        """
        insert into booking_order
            (id, order_no, user_id, session_id, quantity,
             original_amount, discount_amount, total_amount, status,
             payment_no, paid_at, created_at)
        values
            (501, 'ORDER-501', 7, 21, 1, 80.00, 0.00, 80.00, 'PAID', 'PAY-501',
             '2026-08-01 10:00:00', '2026-08-01 09:00:00'),
            (502, 'ORDER-502', 8, 22, 1, 50.00, 0.00, 50.00, 'PENDING_PAYMENT', null,
             null, '2026-08-01 09:30:00'),
            (503, 'ORDER-503', 7, 21, 1, 80.00, 0.00, 80.00, 'CANCELLED', null,
             null, '2026-08-01 09:40:00');
        """,
        "update booking_order set expire_at = '2026-08-01 09:10:00' where id in (501, 502)",
        """
        insert into booking_order_item
            (id, order_id, visitor_id, session_ticket_type_id, visitor_name,
             visitor_id_type, visitor_id_number, ticket_type_name, unit_price)
        values
            (601, 501, 101, 301, '张三', 'ID_CARD',
             '440101199001011234', '成人票', 80.00),
            (603, 503, 103, 301, '王五', 'ID_CARD',
             '440101199001011235', '成人票', 80.00);
        """,
        """
        insert into visitor(id, user_id, name, id_type, id_number, status)
        values (101, 7, '张三', 'ID_CARD', '440101199001011234', 'ACTIVE'),
               (103, 7, '王五', 'ID_CARD', '440101199001011235', 'ACTIVE');
        """,
        """
        insert into visitor_identity(visitor_id, fingerprint)
        values (101, 'fingerprint-101'),
               (103, 'fingerprint-103');
        """,
        """
        insert into ticket
            (id, ticket_code, order_item_id, status, valid_from, valid_until)
        values
            (701, 'TICKET-701', 601, 'VALID',
             '2026-08-10 09:00:00', '2026-08-10 11:00:00');
        """
})
@DisplayName("订单查询 Mapper")
class BookingMapperTest {

    @Autowired
    private BookingMapper bookingMapper;

    @Autowired
    private TicketMapper ticketMapper;

    @Test
    @DisplayName("游客分页查询只返回自己的订单")
    void listMyOrders_shouldFilterByUserAndStatus() {
        List<OrderSummaryVO> orders = bookingMapper.listMyOrders(
                7L, BookingOrderStatus.PAID);

        assertEquals(1, orders.size());
        assertEquals(501L, orders.get(0).getId());
        assertEquals("景点甲", orders.get(0).getVenueName());
    }

    @Test
    @DisplayName("运营端分页查询只返回当前景点订单")
    void listVenueOrders_shouldFilterByVenue() {
        VenueOrderPageQueryDTO queryDTO = new VenueOrderPageQueryDTO();
        queryDTO.setVisitDate(LocalDate.of(2026, 8, 10));
        queryDTO.setStatus(BookingOrderStatus.PAID);

        List<OrderSummaryVO> orders = bookingMapper.listVenueOrders(10L, queryDTO);

        assertEquals(1, orders.size());
        assertEquals(501L, orders.get(0).getId());
    }

    @Test
    @DisplayName("游客详情查询同时校验订单归属")
    void findMyOrderDetail_shouldFilterByUser() {
        assertNotNull(bookingMapper.findMyOrderDetail(501L, 7L));
        assertNull(bookingMapper.findMyOrderDetail(501L, 8L));
    }

    @Test
    @DisplayName("运营端详情返回购买人并限制景点")
    void findVenueOrderDetail_shouldFilterByVenueAndReturnPurchaser() {
        OrderDetailVO detail = bookingMapper.findVenueOrderDetail(501L, 10L);

        assertNotNull(detail);
        assertEquals(7L, detail.getPurchaserUserId());
        assertEquals("游客甲", detail.getPurchaserName());
        assertNull(bookingMapper.findVenueOrderDetail(501L, 20L));
    }

    @Test
    @DisplayName("订单明细关联电子票并只返回脱敏证件号")
    void listOrderItems_shouldMapTicketAndMaskIdNumber() {
        List<OrderItemVO> items = bookingMapper.listOrderItems(501L);

        assertEquals(1, items.size());
        assertEquals("440***********1234", items.get(0).getMaskedVisitorIdNumber());
        assertNotNull(items.get(0).getTicket());
        assertEquals(TicketStatus.VALID, items.get(0).getTicket().getStatus());
    }

    @Test
    @DisplayName("查询订单票券并批量作废有效票券")
    void ticketMapper_shouldListAndVoidValidTickets() {
        List<Ticket> tickets = ticketMapper.listTicketsByOrderId(501L);
        assertEquals(1, tickets.size());
        assertEquals(TicketStatus.VALID, tickets.get(0).getStatus());

        tickets.get(0).setStatus(TicketStatus.VOID);
        assertEquals(1, ticketMapper.updateTickets(tickets));
        assertEquals(
                TicketStatus.VOID,
                ticketMapper.listTicketsByOrderId(501L).get(0).getStatus());
    }

    @Test
    @DisplayName("退款票券可以冻结并在成功后作废")
    void refundTicket_shouldFreezeAndVoid() {
        assertEquals(1, ticketMapper.markRefundingByOrderId(501L));
        assertEquals(
                TicketStatus.REFUNDING,
                ticketMapper.listTicketsByOrderId(501L).get(0).getStatus());

        assertEquals(1, ticketMapper.voidRefundingByOrderId(501L));
        assertEquals(
                TicketStatus.VOID,
                ticketMapper.listTicketsByOrderId(501L).get(0).getStatus());
    }

    @Test
    @DisplayName("可按申请时间扫描退款中订单")
    void listRefundingOrders_shouldUseRequestedTime() {
        BookingOrder order = bookingMapper.findOrderByOrderId(501L);
        order.setStatus(BookingOrderStatus.REFUNDING);
        order.setRefundNo("RF-501");
        order.setRefundRequestedAt(LocalDateTime.of(2026, 8, 1, 10, 0));
        assertEquals(1, bookingMapper.updateOrder(order, BookingOrderStatus.PAID));

        List<BookingOrder> orders = bookingMapper.listRefundingOrders(
                LocalDateTime.of(2026, 8, 1, 10, 1));

        assertEquals(1, orders.size());
        assertEquals("RF-501", orders.get(0).getRefundNo());
    }

    @Test
    @DisplayName("超时查询只返回到期的待支付订单")
    void listTimeoutOrders_shouldOnlyReturnPendingPaymentOrders() {
        List<com.qinghuan.pojo.entity.BookingOrder> orders = bookingMapper.listTimeoutOrders(
                LocalDateTime.of(2026, 8, 1, 10, 0));

        assertEquals(1, orders.size());
        assertEquals(502L, orders.get(0).getId());
        assertEquals(BookingOrderStatus.PENDING_PAYMENT, orders.get(0).getStatus());
    }

    @Test
    @DisplayName("一人一场次查询只返回仍占用下单资格的订单")
    void findConflictingOrders_shouldMatchSessionAndFingerprint() {
        assertEquals(1, bookingMapper.findConflictingOrdersBySessionAndFingerprints(
                21L, List.of("fingerprint-101")).size());
        assertEquals(0, bookingMapper.findConflictingOrdersBySessionAndFingerprints(
                21L, List.of("fingerprint-103")).size());
        assertEquals(0, bookingMapper.findConflictingOrdersBySessionAndFingerprints(
                22L, List.of("fingerprint-101")).size());
        assertEquals(0, bookingMapper.findConflictingOrdersBySessionAndFingerprints(
                21L, List.of("fingerprint-999")).size());
    }

    @Test
    @DisplayName("支付更新要求订单待支付且支付时间早于过期时间")
    void updatePaidOrder_shouldCheckStatusAndExpireAt() {
        BookingOrder order = bookingMapper.findOrderByOrderId(502L);
        order.setStatus(BookingOrderStatus.PAID);
        order.setPaymentNo("PAY-502");
        order.setPaidAt(LocalDateTime.of(2026, 8, 1, 9, 0));

        int updated = bookingMapper.updatePaidOrder(
                order, BookingOrderStatus.PENDING_PAYMENT);

        assertEquals(1, updated);
        BookingOrder paidOrder = bookingMapper.findOrderByOrderId(502L);
        assertEquals(BookingOrderStatus.PAID, paidOrder.getStatus());
        assertEquals("PAY-502", paidOrder.getPaymentNo());
        assertNotNull(paidOrder.getPaidAt());
    }

    @Test
    @DisplayName("支付时间到期后条件更新失败")
    void updatePaidOrder_shouldRejectExpiredOrder() {
        BookingOrder order = bookingMapper.findOrderByOrderId(502L);
        order.setStatus(BookingOrderStatus.PAID);
        order.setPaymentNo("PAY-502");
        order.setPaidAt(LocalDateTime.of(2026, 8, 1, 9, 10));

        int updated = bookingMapper.updatePaidOrder(
                order, BookingOrderStatus.PENDING_PAYMENT);

        assertEquals(0, updated);
        assertEquals(
                BookingOrderStatus.PENDING_PAYMENT,
                bookingMapper.findOrderByOrderId(502L).getStatus());
    }
}
