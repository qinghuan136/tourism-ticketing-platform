package com.qinghuan.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 场次中的单个票种售价和配额配置。
 */
@Getter
@Setter
@Schema(description = "场次中的票种售价与发行配额")
public class SessionTicketTypeConfigDTO {

    @Schema(description = "当前景点已有的基础票种 ID", example = "1")
    @NotNull(message = "票种ID不能为空")
    @Positive(message = "票种ID必须为正数")
    private Long ticketTypeId;

    @Schema(description = "该票种在本场次的销售价格", example = "50.00")
    @NotNull(message = "场次售价不能为空")
    @DecimalMin(value = "0.00", message = "场次售价不能小于0")
    @Digits(integer = 8, fraction = 2, message = "场次售价最多为8位整数和2位小数")
    private BigDecimal salePrice;

    @Schema(description = "该票种在本场次的发行数量", example = "200")
    @NotNull(message = "票种分配数量不能为空")
    @Positive(message = "票种分配数量必须大于0")
    private Integer allocatedQuantity;
}
