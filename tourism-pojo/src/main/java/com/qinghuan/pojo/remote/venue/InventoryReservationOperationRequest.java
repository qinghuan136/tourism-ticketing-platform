package com.qinghuan.pojo.remote.venue;

/** 对已创建的库存预留执行 Confirm 或 Cancel。 */
public record InventoryReservationOperationRequest(Long orderId) {
}
