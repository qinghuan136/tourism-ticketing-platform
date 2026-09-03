package com.qinghuan.pojo.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单详情。
 * 游客查询时购买人字段为空；运营端查询时补充购买人概要。
 */
@Getter
@Setter
public class OrderDetailVO extends OrderSummaryVO {

    private Long purchaserUserId;
    private String purchaserName;
    private String purchaserPhone;
    private String paymentNo;
    private String refundNo;
    private LocalDateTime paidAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime closedAt;
    private LocalDateTime completedAt;
    private LocalDateTime refundRequestedAt;
    private LocalDateTime refundAt;
    private List<OrderItemVO> items;
}
