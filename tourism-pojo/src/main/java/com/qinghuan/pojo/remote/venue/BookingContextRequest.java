package com.qinghuan.pojo.remote.venue;

import java.util.List;

/** 订单服务向景点服务查询场次下单上下文的请求。 */
public record BookingContextRequest(
        Long sessionId,
        List<Long> sessionTicketTypeIds) {
}
