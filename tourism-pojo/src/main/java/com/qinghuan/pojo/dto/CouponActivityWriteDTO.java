package com.qinghuan.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 创建或修改优惠券活动草稿的请求。 */
@Getter
@Setter
@Schema(description = "创建或修改优惠券活动草稿的参数")
public class CouponActivityWriteDTO {
    @Schema(description = "活动名称", example = "国庆满减券")
    @NotBlank(message = "活动名称不能为空")
    @Size(max = 100, message = "活动名称不能超过100个字符")
    private String name;

    @Schema(description = "订单达到该金额后可使用", example = "100.00")
    @NotNull(message = "使用门槛不能为空")
    @DecimalMin(value = "0.00", message = "使用门槛不能小于0")
    @Digits(integer = 8, fraction = 2, message = "使用门槛最多保留两位小数")
    private BigDecimal thresholdAmount;

    @Schema(description = "优惠金额，必须大于 0", example = "20.00")
    @NotNull(message = "优惠金额不能为空")
    @DecimalMin(value = "0.00", inclusive = false, message = "优惠金额必须大于0")
    @Digits(integer = 8, fraction = 2, message = "优惠金额最多保留两位小数")
    private BigDecimal discountAmount;

    @Schema(description = "优惠券发行总量", example = "1000")
    @NotNull(message = "发行量不能为空")
    @Positive(message = "发行量必须大于0")
    @Max(value = 1_000_000, message = "发行量不能超过1000000")
    private Integer totalStock;

    @Schema(description = "游客可开始领取的时间", example = "2026-09-20T10:00:00")
    @NotNull(message = "领取开始时间不能为空")
    private LocalDateTime claimStartAt;
    @Schema(description = "游客停止领取的时间", example = "2026-09-30T23:59:59")
    @NotNull(message = "领取结束时间不能为空")
    private LocalDateTime claimEndAt;
    @Schema(description = "优惠券开始可用于订单的时间", example = "2026-10-01T00:00:00")
    @NotNull(message = "生效时间不能为空")
    private LocalDateTime validFrom;
    @Schema(description = "优惠券失效时间", example = "2026-10-07T23:59:59")
    @NotNull(message = "失效时间不能为空")
    private LocalDateTime validUntil;
}
