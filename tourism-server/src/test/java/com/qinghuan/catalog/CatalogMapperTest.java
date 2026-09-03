package com.qinghuan.catalog;

import com.qinghuan.pojo.dto.CatalogVenuePageQueryDTO;
import com.qinghuan.pojo.vo.CatalogVenueVO;
import com.qinghuan.pojo.vo.SellableSessionVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@MybatisTest(properties = {
        "mybatis.mapper-locations=classpath:mapper/**/*.xml",
        "mybatis.configuration.map-underscore-to-camel-case=true"
})
@Sql(statements = {
        "drop table if exists session_ticket_type",
        "drop table if exists ticket_type",
        "drop table if exists admission_session",
        "drop table if exists venue",
        """
        create table venue (
            id bigint primary key,
            name varchar(100) not null,
            address varchar(255) not null,
            description varchar(500),
            cover_object_key varchar(500),
            status varchar(20) not null,
            longitude decimal(10, 7),
            latitude decimal(9, 7)
        );
        """,
        """
        create table admission_session (
            id bigint primary key,
            venue_id bigint not null,
            visit_date date not null,
            start_time time not null,
            end_time time not null,
            booking_start_at timestamp not null,
            booking_end_at timestamp not null,
            total_capacity int not null,
            remaining_capacity int not null,
            status varchar(20) not null
        );
        """,
        """
        create table ticket_type (
            id bigint primary key,
            venue_id bigint not null,
            name varchar(100) not null,
            description varchar(500),
            audience_rule varchar(500),
            base_price decimal(10, 2) not null,
            status varchar(20) not null
        );
        """,
        """
        create table session_ticket_type (
            id bigint primary key,
            session_id bigint not null,
            ticket_type_id bigint not null,
            sale_price decimal(10, 2) not null,
            allocated_quantity int not null,
            remaining_quantity int not null,
            status varchar(20) not null
        );
        """,
        """
        insert into venue(id, name, address, description, cover_object_key, status, longitude, latitude)
        values (10, '海湾科技馆', '广州市番禺区海湾大道 88 号', '科技场馆', 'venue/a.jpg', 'ENABLED', 113.32, 23.10),
               (20, '停用景点', '深圳市测试路 1 号', '已停用', null, 'DISABLED', 114.05, 22.54),
               (30, '广州历史展馆', '广州市测试路 2 号', '暂未开放预约', null, 'ENABLED', 113.31, 23.11);
        """,
        """
        insert into admission_session
            (id, venue_id, visit_date, start_time, end_time, booking_start_at,
             booking_end_at, total_capacity, remaining_capacity, status)
        values
            (101, 10, DATEADD('DAY', 1, CURRENT_DATE), '09:00:00', '11:00:00',
             DATEADD('HOUR', -1, CURRENT_TIMESTAMP), DATEADD('HOUR', 1, CURRENT_TIMESTAMP),
             100, 24, 'OPEN'),
            (102, 10, DATEADD('DAY', 1, CURRENT_DATE), '12:00:00', '14:00:00',
             DATEADD('HOUR', -1, CURRENT_TIMESTAMP), DATEADD('HOUR', 1, CURRENT_TIMESTAMP),
             100, 30, 'CLOSED'),
            (103, 10, DATEADD('DAY', 1, CURRENT_DATE), '15:00:00', '17:00:00',
             DATEADD('HOUR', -2, CURRENT_TIMESTAMP), DATEADD('HOUR', -1, CURRENT_TIMESTAMP),
             100, 40, 'OPEN'),
            (104, 20, DATEADD('DAY', 1, CURRENT_DATE), '09:00:00', '11:00:00',
             DATEADD('HOUR', -1, CURRENT_TIMESTAMP), DATEADD('HOUR', 1, CURRENT_TIMESTAMP),
             100, 50, 'OPEN');
        """,
        """
        insert into ticket_type
            (id, venue_id, name, description, audience_rule, base_price, status)
        values
            (201, 10, '成人票', '普通参观票', '普通成年游客', 80.00, 'ENABLED'),
            (202, 10, '儿童票', '儿童优惠票', '1.2 米以下儿童', 0.00, 'ENABLED'),
            (203, 10, '停用票', '不可售', null, 20.00, 'DISABLED'),
            (204, 20, '异地票', '不可售', null, 30.00, 'ENABLED');
        """,
        """
        insert into session_ticket_type
            (id, session_id, ticket_type_id, sale_price, allocated_quantity,
             remaining_quantity, status)
        values
            (301, 101, 201, 60.00, 40, 20, 'ON_SALE'),
            (302, 101, 202, 0.00, 10, 4, 'ON_SALE'),
            (303, 101, 203, 20.00, 10, 5, 'ON_SALE'),
            (304, 102, 201, 60.00, 40, 20, 'ON_SALE'),
            (305, 103, 201, 60.00, 40, 20, 'ON_SALE'),
            (306, 104, 204, 30.00, 40, 20, 'ON_SALE');
        """
})
@DisplayName("游客端可售内容 Mapper")
class CatalogMapperTest {

    @Autowired
    private CatalogMapper catalogMapper;

    @Test
    void listEnabledVenues_shouldIncludeVenueWithoutCurrentInventory() {
        CatalogVenuePageQueryDTO query = new CatalogVenuePageQueryDTO();
        query.setKeyword("广州");

        List<CatalogVenueVO> venues = catalogMapper.listEnabledVenues(query);

        assertEquals(2, venues.size());
        assertEquals(10L, venues.get(0).getId());
        assertEquals(new BigDecimal("0.00"), venues.get(0).getMinimumPrice());
        assertEquals("venue/a.jpg", venues.get(0).getCoverUrl());
        assertEquals(30L, venues.get(1).getId());
        assertNull(venues.get(1).getMinimumPrice());
    }

    @Test
    void findEnabledVenue_shouldHideDisabledVenue() {
        assertEquals(10L, catalogMapper.findEnabledVenue(10L).getId());
        assertNull(catalogMapper.findEnabledVenue(20L));
    }

    @Test
    void listSellableSessions_shouldNestOnlySellableTicketTypes() {
        List<SellableSessionVO> sessions = catalogMapper.listSellableSessions(
                10L, LocalDate.now().plusDays(1));

        assertEquals(1, sessions.size());
        assertEquals(101L, sessions.get(0).getId());
        assertEquals(24, sessions.get(0).getRemainingCapacity());
        assertEquals(2, sessions.get(0).getTicketTypes().size());
        assertEquals(301L, sessions.get(0).getTicketTypes().get(0).getSessionTicketTypeId());
        assertEquals("儿童票", sessions.get(0).getTicketTypes().get(1).getTicketTypeName());
    }
}
