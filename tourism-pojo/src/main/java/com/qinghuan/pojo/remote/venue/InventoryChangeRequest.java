package com.qinghuan.pojo.remote.venue;

import java.util.Map;

/** 订单创建或关闭时对场次库存执行的一次变更。 */
public record InventoryChangeRequest(
        Long sessionId,
        Map<Long, Integer> ticketTypeQuantities) {
}
