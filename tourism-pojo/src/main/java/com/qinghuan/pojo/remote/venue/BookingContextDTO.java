package com.qinghuan.pojo.remote.venue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** 景点服务一次性返回给订单服务的场次与票种下单上下文。 */
public record BookingContextDTO(
        Long sessionId,
        Long venueId,
        String venueName,
        String venueAddress,
        LocalDate visitDate,
        LocalTime startTime,
        LocalTime endTime,
        LocalDateTime sessionStartTime,
        List<BookingTicketTypeDTO> ticketTypes) {
}
