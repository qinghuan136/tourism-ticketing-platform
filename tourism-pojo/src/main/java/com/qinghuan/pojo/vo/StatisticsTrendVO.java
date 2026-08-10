package com.qinghuan.pojo.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 单日订单和销售趋势数据。 */
@Getter
@Setter
public class StatisticsTrendVO {
    private LocalDate statisticDate;
    private Long paidOrderCount;
    private BigDecimal grossRevenue;
    private Long soldTicketCount;
}
