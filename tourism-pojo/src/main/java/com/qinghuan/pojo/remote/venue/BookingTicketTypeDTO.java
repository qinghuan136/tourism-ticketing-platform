package com.qinghuan.pojo.remote.venue;

import java.math.BigDecimal;

/** 下单所需的场次票种不可变快照。 */
public record BookingTicketTypeDTO(
        Long sessionTicketTypeId,
        String ticketTypeName,
        BigDecimal salePrice) {
}
