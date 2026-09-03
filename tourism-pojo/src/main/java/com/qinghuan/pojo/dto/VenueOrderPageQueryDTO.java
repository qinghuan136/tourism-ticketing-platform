package com.qinghuan.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** 运营端订单分页条件，查询范围始终限制在当前账号所属景点。 */
@Getter
@Setter
public class VenueOrderPageQueryDTO extends OrderPageQueryDTO {

    @Schema(description = "订单号模糊筛选", example = "BK2026")
    @Size(max = 32, message = "订单号长度不能超过32个字符")
    private String orderNo;

    @Schema(description = "场次 ID 筛选", example = "1")
    @Positive(message = "场次ID必须为正数")
    private Long sessionId;

    @Schema(description = "参观日期筛选", example = "2026-10-01")
    private LocalDate visitDate;
}
