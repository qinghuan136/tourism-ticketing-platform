package com.qinghuan.pojo.entity;

import com.qinghuan.pojo.enums.InventoryReservationStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 一笔订单在场次服务中的库存预留主记录。 */
@Getter
@Setter
public class InventoryReservationOrder {

    private Long orderId;
    private Long sessionId;
    private InventoryReservationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
