package com.qinghuan.verification;

import com.qinghuan.pojo.dto.TicketPageQueryDTO;
import com.qinghuan.pojo.dto.VerificationPageQueryDTO;
import com.qinghuan.pojo.entity.VerificationRecord;
import com.qinghuan.pojo.enums.TicketStatus;
import com.qinghuan.pojo.enums.VerificationResult;
import com.qinghuan.pojo.vo.TicketVO;
import com.qinghuan.pojo.vo.TicketVerificationInfo;
import com.qinghuan.pojo.vo.VerificationRecordVO;
import com.qinghuan.ticket.TicketMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

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
        "drop table if exists verification_record",
        "drop table if exists ticket",
        "drop table if exists booking_order_item",
        "drop table if exists booking_order",
        "drop table if exists admission_session",
        "drop table if exists user_account",
        "drop table if exists venue",
        """
        create table venue (
            id bigint primary key, name varchar(100) not null, address varchar(255) not null
        );
        """,
        """
        create table user_account (
            id bigint primary key, display_name varchar(50) not null
        );
        """,
        """
        create table admission_session (
            id bigint primary key, venue_id bigint not null, visit_date date not null,
            start_time time not null, end_time time not null
        );
        """,
        """
        create table booking_order (
            id bigint primary key, order_no varchar(32) not null,
            user_id bigint not null, session_id bigint not null,
            venue_id bigint not null, venue_name_snapshot varchar(100) not null,
            venue_address_snapshot varchar(255), visit_date date not null,
            start_time time not null, end_time time not null
        );
        """,
        """
        create table booking_order_item (
            id bigint primary key, order_id bigint not null, visitor_id bigint not null,
            visitor_name varchar(50) not null, ticket_type_name varchar(100) not null
        );
        """,
        """
        create table ticket (
            id bigint primary key, ticket_code varchar(64) not null unique,
            order_item_id bigint not null, status varchar(20) not null,
            valid_from timestamp not null, valid_until timestamp not null,
            verified_at timestamp, created_at timestamp default current_timestamp,
            updated_at timestamp default current_timestamp
        );
        """,
        """
        create table verification_record (
            id bigint auto_increment primary key, request_no varchar(64) not null unique,
            ticket_id bigint not null, verifier_id bigint not null, verifier_name_snapshot varchar(50), result varchar(20) not null,
            failure_reason varchar(255), device_no varchar(64), verified_at timestamp not null
        );
        """,
        "insert into venue values (10, '海湾科技馆', '广州测试路 1 号'), (20, '异地景点', '深圳测试路 2 号')",
        "insert into user_account values (31, '检票员01'), (32, '检票员02')",
        "insert into admission_session values (21, 10, CURRENT_DATE, '09:00:00', '11:00:00'), (22, 20, CURRENT_DATE, '14:00:00', '16:00:00')",
        "insert into booking_order values (501, 'ORDER-501', 7, 21, 10, '海湾科技馆', '广州测试路 1 号', CURRENT_DATE, '09:00:00', '11:00:00'), (502, 'ORDER-502', 8, 22, 20, '异地景点', '深圳测试路 2 号', CURRENT_DATE, '14:00:00', '16:00:00')",
        "insert into booking_order_item values (601, 501, 101, '张三', '成人票'), (602, 502, 102, '李四', '学生票')",
        """
        insert into ticket
            (id, ticket_code, order_item_id, status, valid_from, valid_until)
        values
            (701, 'TK-701', 601, 'VALID', DATEADD('HOUR', -1, CURRENT_TIMESTAMP), DATEADD('HOUR', 1, CURRENT_TIMESTAMP)),
            (702, 'TK-702', 602, 'VALID', DATEADD('HOUR', -1, CURRENT_TIMESTAMP), DATEADD('HOUR', 1, CURRENT_TIMESTAMP));
        """,
        """
        insert into verification_record
            (id, request_no, ticket_id, verifier_id, verifier_name_snapshot, result, failure_reason, device_no, verified_at)
        values
            (801, 'REQ-OLD', 701, 31, '检票员01', 'FAILED', '票券尚未生效', 'GATE-01', CURRENT_TIMESTAMP);
        """
})
@DisplayName("电子票查询与核销 Mapper")
class TicketVerificationMapperTest {

    @Autowired
    private TicketMapper ticketMapper;
    @Autowired
    private VerificationMapper verificationMapper;

    @Test
    void ticketQuery_shouldRestrictCurrentTourist() {
        TicketPageQueryDTO query = new TicketPageQueryDTO();
        query.setStatus(TicketStatus.VALID);

        List<TicketVO> tickets = ticketMapper.listMyTickets(7L, query);

        assertEquals(1, tickets.size());
        assertEquals(701L, tickets.get(0).getId());
        assertNotNull(ticketMapper.findMyTicket(701L, 7L));
        assertNull(ticketMapper.findMyTicket(701L, 8L));
    }

    @Test
    void ticketVerification_shouldRestrictVenueAndConditionallyMarkUsed() {
        TicketVerificationInfo ticket =
                ticketMapper.findForVerificationByCode("TK-701", 10L);

        assertNotNull(ticket);
        assertNull(ticketMapper.findForVerificationByCode("TK-701", 20L));
        assertEquals(1, ticketMapper.markUsed(701L, LocalDateTime.now()));
        assertEquals(0, ticketMapper.markUsed(701L, LocalDateTime.now()));
        assertEquals(
                TicketStatus.USED,
                ticketMapper.findForVerificationById(701L, 10L).getStatus());
    }

    @Test
    void verificationRecord_shouldInsertAndFindByRequestNo() {
        VerificationRecord record = new VerificationRecord();
        record.setRequestNo("REQ-NEW");
        record.setTicketId(701L);
        record.setVerifierId(31L);
        record.setVerifierNameSnapshot("检票员01");
        record.setResult(VerificationResult.SUCCESS);
        record.setDeviceNo("GATE-01");
        record.setVerifiedAt(LocalDateTime.now());

        assertEquals(1, verificationMapper.insertRecord(record));

        VerificationRecord saved = verificationMapper.findByRequestNo("REQ-NEW");
        assertNotNull(saved.getId());
        assertEquals(VerificationResult.SUCCESS, saved.getResult());

        saved.setResult(VerificationResult.FAILED);
        saved.setFailureReason("票券状态发生变化");
        assertEquals(1, verificationMapper.updateResult(saved));
        VerificationRecord updated =
                verificationMapper.findByRequestNoForUpdate("REQ-NEW");
        assertEquals(VerificationResult.FAILED, updated.getResult());
        assertEquals("票券状态发生变化", updated.getFailureReason());
    }

    @Test
    void verificationRecordList_shouldRestrictVenueAndApplyFilters() {
        VerificationPageQueryDTO query = new VerificationPageQueryDTO();
        query.setTicketCode("TK-701");
        query.setResult(VerificationResult.FAILED);
        query.setVerificationDate(LocalDate.now());

        List<VerificationRecordVO> records =
                verificationMapper.listRecords(10L, query);

        assertEquals(1, records.size());
        assertEquals("检票员01", records.get(0).getVerifierName());
        assertEquals("票券尚未生效", records.get(0).getFailureReason());
        assertEquals(0, verificationMapper.listRecords(20L, query).size());
    }
}
