package com.qinghuan.pojo.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 游客端展示的场次票种。
 *
 * remainingQuantity在尚未开售时为null。
 */
@Getter
@Setter
public class SellableTicketTypeVO {

    private Long sessionTicketTypeId;
    private String ticketTypeName;
    private String description;
    private String audienceRule;
    private BigDecimal salePrice;

    /** 未开售时不返回，开售后才补充。 */
    private Integer remainingQuantity;
}
