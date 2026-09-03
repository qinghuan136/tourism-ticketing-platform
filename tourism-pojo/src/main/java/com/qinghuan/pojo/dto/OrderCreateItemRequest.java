package com.qinghuan.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 单张订单明细的选择信息。
 * sessionTicketTypeId 指向场次票种配置，而不是普通票种 ID。
 */
@Schema(description = "单张订单明细的参观人与场次票种选择")
public record OrderCreateItemRequest(
        @Schema(description = "当前游客名下的参观人 ID", example = "4")
        @NotNull @Positive Long visitorId,
        @Schema(description = "场次票种配置 ID，不是基础票种 ID", example = "2")
        @NotNull @Positive Long sessionTicketTypeId) {
}
