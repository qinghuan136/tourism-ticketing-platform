package com.qinghuan.pojo.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 单个场次票种的动态库存。
 */
@Getter
@Setter
public class SessionTicketTypeInventoryVO {

    private Long sessionTicketTypeId;
    private Integer remainingQuantity;
}