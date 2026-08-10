package com.qinghuan.pojo.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 游客端使用的场次静态快照。
 *
 * 该对象可以在开售前和开售后复用，
 * 实时库存需要通过其他查询额外补充。
 */
@Getter
@Setter
public class SessionStaticSnapshotVO {

    private Long sessionId;
    private Long venueId;

    private LocalDate visitDate;
    private LocalTime startTime;
    private LocalTime endTime;

    private LocalDateTime bookingStartAt;
    private LocalDateTime bookingEndAt;

    private List<SessionTicketTypeStaticVO> ticketTypes;
}