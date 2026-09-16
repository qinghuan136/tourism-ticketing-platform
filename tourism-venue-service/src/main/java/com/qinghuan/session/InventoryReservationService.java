package com.qinghuan.session;

import java.util.Map;

/** 场次库存的 Try / Confirm / Cancel 二阶段操作。 */
public interface InventoryReservationService {

    void tryReserve(Long orderId, Long sessionId, Map<Long, Integer> ticketTypeQuantities);

    void confirmReserve(Long orderId);

    void cancelReserve(Long orderId);
}
