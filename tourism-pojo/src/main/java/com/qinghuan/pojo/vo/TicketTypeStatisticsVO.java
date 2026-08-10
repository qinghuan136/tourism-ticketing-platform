package com.qinghuan.pojo.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** 按订单明细快照汇总的票种销售数据。 */
@Getter
@Setter
public class TicketTypeStatisticsVO {
    private String ticketTypeName;
    private Long soldQuantity;
    private BigDecimal salesAmount;
}
