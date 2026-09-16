package com.qinghuan.pojo.remote.venue;

import java.util.Map;

/** 订单服务请求场次服务执行库存 Try 预留。 */
public record InventoryTryReserveRequest(
        Long orderId,
        Long sessionId,
        Map<Long, Integer> ticketTypeQuantities) {
}
