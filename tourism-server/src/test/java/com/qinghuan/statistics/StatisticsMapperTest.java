package com.qinghuan.statistics;

import com.qinghuan.pojo.vo.StatisticsOverviewVO;
import com.qinghuan.pojo.vo.StatisticsTrendVO;
import com.qinghuan.pojo.vo.TicketTypeStatisticsVO;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MybatisTest(properties = {
        "mybatis.mapper-locations=classpath:mapper/**/*.xml",
        "mybatis.configuration.map-underscore-to-camel-case=true"
})
@Sql(statements = {
        "drop table if exists ticket",
        "drop table if exists booking_order_item",
        "drop table if exists booking_order",
        "drop table if exists admission_session",
        "create table admission_session (id bigint primary key, venue_id bigint not null);",
        """
        create table booking_order (
            id bigint primary key,
            session_id bigint not null,
            quantity int not null,
            total_amount decimal(10, 2) not null,
            paid_at timestamp,
            refund_at timestamp
        );
        """,
        """
        create table booking_order_item (
            id bigint primary key,
            order_id bigint not null,
            ticket_type_name varchar(100) not null,
            unit_price decimal(10, 2) not null
        );
        """,
        "create table ticket (id bigint primary key, order_item_id bigint not null, verified_at timestamp);",
        "insert into admission_session(id, venue_id) values (10, 1), (20, 2);",
        """
        insert into booking_order(id, session_id, quantity, total_amount, paid_at, refund_at) values
            (1, 10, 2, 100.00, '2026-08-02 10:00:00', null),
            (2, 10, 1, 50.00, '2026-08-02 11:00:00', '2026-08-03 12:00:00'),
            (3, 10, 1, 30.00, '2026-07-31 10:00:00', null),
            (4, 20, 1, 80.00, '2026-08-02 10:00:00', null);
        """,
        """
        insert into booking_order_item(id, order_id, ticket_type_name, unit_price) values
            (11, 1, '成人票', 60.00),
            (12, 1, '儿童票', 40.00),
            (13, 2, '成人票', 50.00),
            (14, 3, '成人票', 30.00),
            (15, 4, '成人票', 80.00);
        """,
        """
        insert into ticket(id, order_item_id, verified_at) values
            (101, 11, '2026-08-02 13:00:00'),
            (102, 12, null),
            (103, 13, '2026-08-04 09:00:00');
        """
})
class StatisticsMapperTest {

    private static final LocalDateTime START = LocalDateTime.parse("2026-08-01T00:00:00");
    private static final LocalDateTime END = LocalDateTime.parse("2026-08-04T00:00:00");

    @Autowired
    private StatisticsMapper statisticsMapper;

    @Test
    void getOverview_shouldOnlyAggregateCurrentVenueAndRange() {
        StatisticsOverviewVO overview = statisticsMapper.getOverview(1L, START, END);

        assertEquals(2L, overview.getPaidOrderCount());
        assertEquals(new BigDecimal("150.00"), overview.getGrossRevenue());
        assertEquals(new BigDecimal("50.00"), overview.getRefundAmount());
        assertEquals(3L, overview.getSoldTicketCount());
        assertEquals(1L, overview.getVerifiedTicketCount());
    }

    @Test
    void listTrend_shouldGroupByPaymentDate() {
        List<StatisticsTrendVO> trend = statisticsMapper.listTrend(1L, START, END);

        assertEquals(1, trend.size());
        assertEquals("2026-08-02", trend.get(0).getStatisticDate().toString());
        assertEquals(2L, trend.get(0).getPaidOrderCount());
        assertEquals(new BigDecimal("150.00"), trend.get(0).getGrossRevenue());
        assertEquals(3L, trend.get(0).getSoldTicketCount());
    }

    @Test
    void listTicketTypeStatistics_shouldUseOrderItemSnapshots() {
        List<TicketTypeStatisticsVO> result =
                statisticsMapper.listTicketTypeStatistics(1L, START, END);

        assertEquals(2, result.size());
        assertEquals("成人票", result.get(0).getTicketTypeName());
        assertEquals(2L, result.get(0).getSoldQuantity());
        assertEquals(new BigDecimal("110.00"), result.get(0).getSalesAmount());
        assertEquals("儿童票", result.get(1).getTicketTypeName());
    }
}
