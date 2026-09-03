package com.qinghuan.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * 创建订单请求。
 * items 中的每一项代表“一个参观人购买一张场次票”。
 */
@Schema(description = "创建订单参数；每个明细代表一个参观人购买一张场次票")
public record OrderCreateDTO(
        @Schema(description = "下单场次 ID", example = "1")
        @NotNull @Positive Long sessionId,
        @Schema(description = "订单明细，至少包含一个参观人与场次票种组合")
        @NotEmpty @Valid List<OrderCreateItemRequest> items,
        // 可选；传入时由后端校验归属、景点、有效期和使用门槛。
        @Schema(description = "游客已领取优惠券 ID；不使用优惠券时不传", example = "3")
        @Positive Long userCouponId) {

}
