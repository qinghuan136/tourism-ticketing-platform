package com.qinghuan.pojo.vo;

import com.qinghuan.pojo.enums.CatalogSessionSaleState;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 游客端可展示的场次。
 *
 * 未开售时只返回静态信息；
 * 开售后补充场次容量和票种库存。
 */
@Getter
@Setter
public class SellableSessionVO {

    private Long id;
    private LocalDate visitDate;
    private LocalTime startTime;
    private LocalTime endTime;

    /** 预约开放时间，前端可以用于倒计时展示。 */
    private LocalDateTime bookingStartAt;

    private LocalDateTime bookingEndAt;

    private CatalogSessionSaleState saleState;

    /**
     * 未开售时为null；
     * 开售后返回当前剩余场次容量。
     */
    private Integer remainingCapacity;

    private List<SellableTicketTypeVO> ticketTypes;
}
