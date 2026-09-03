package com.qinghuan.pojo.dto;

import com.qinghuan.pojo.enums.UserCouponStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** 查询当前游客优惠券的可选筛选条件。 */
@Getter
@Setter
public class UserCouponQueryDTO {
    @Schema(description = "所属景点 ID 筛选", example = "1")
    @Positive(message = "景点ID必须为正数")
    private Long venueId;
    @Schema(description = "优惠券状态筛选")
    private UserCouponStatus status;
    @Schema(description = "订单金额；传入后仅返回达到使用门槛的优惠券", example = "100.00")
    @DecimalMin(value = "0.00", message = "订单金额不能小于0")
    @Digits(integer = 8, fraction = 2, message = "订单金额最多保留两位小数")
    private BigDecimal orderAmount;
}
