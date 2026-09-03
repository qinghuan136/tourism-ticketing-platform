package com.qinghuan.pojo.dto;

import com.qinghuan.pojo.enums.BookingOrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/** 游客订单分页条件，当前 MVP 只支持按状态筛选。 */
@Getter
@Setter
public class OrderPageQueryDTO extends PageQuery {

    @Schema(description = "订单状态筛选，不传则查询全部状态")
    private BookingOrderStatus status;
}
