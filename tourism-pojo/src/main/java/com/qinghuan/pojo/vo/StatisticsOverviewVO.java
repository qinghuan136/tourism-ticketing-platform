package com.qinghuan.pojo.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** 运营端核心经营指标概览。 */
@Getter
@Setter
public class StatisticsOverviewVO {
    private Long paidOrderCount;
    private BigDecimal grossRevenue;
    private BigDecimal refundAmount;
    private BigDecimal netRevenue;
    private Long soldTicketCount;
    private Long verifiedTicketCount;
}
