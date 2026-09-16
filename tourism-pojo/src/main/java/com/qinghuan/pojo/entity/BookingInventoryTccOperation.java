package com.qinghuan.pojo.entity;

import com.qinghuan.pojo.enums.BookingInventoryTccStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 订单服务持久化的库存 TCC 协调记录。 */
@Getter
@Setter
public class BookingInventoryTccOperation {

    private Long orderId;
    private Long sessionId;
    private BookingInventoryTccStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
