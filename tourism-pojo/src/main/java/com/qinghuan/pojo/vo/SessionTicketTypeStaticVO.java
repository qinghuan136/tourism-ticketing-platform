package com.qinghuan.pojo.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 场次票种的静态展示信息。
 *
 * 不包含剩余库存，开售后仍可以继续复用。
 */
@Getter
@Setter
public class SessionTicketTypeStaticVO {

    /** 创建订单时需要提交的场次票种关联ID。 */
    private Long sessionTicketTypeId;

    /** 基础票种ID。 */
    private Long ticketTypeId;

    private String ticketTypeName;
    private String description;
    private String audienceRule;

    /** 场次实际售价，而不是基础票种的默认价格。 */
    private BigDecimal salePrice;
}