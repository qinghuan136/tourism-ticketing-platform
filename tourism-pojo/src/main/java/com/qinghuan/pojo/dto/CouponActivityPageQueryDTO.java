package com.qinghuan.pojo.dto;

import com.qinghuan.pojo.enums.CouponActivityStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** 运营端优惠券活动分页筛选条件。 */
@Getter
@Setter
public class CouponActivityPageQueryDTO extends PageQuery {
    @Schema(description = "活动状态筛选")
    private CouponActivityStatus status;

    @Schema(description = "按活动名称模糊搜索", example = "国庆")
    @Size(max = 100, message = "关键字不能超过100个字符")
    private String keyword;
}
