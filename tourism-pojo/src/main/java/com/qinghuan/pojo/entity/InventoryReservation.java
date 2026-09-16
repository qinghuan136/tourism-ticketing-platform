package com.qinghuan.pojo.entity;

import com.qinghuan.pojo.enums.InventoryReservationStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 一笔订单对某个场次票种的库存预留明细。 */
@Getter
@Setter
public class InventoryReservation {

    private Long orderId;
    private Long sessionTicketTypeId;
    private Integer quantity;
    private InventoryReservationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
