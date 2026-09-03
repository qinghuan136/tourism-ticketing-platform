package com.qinghuan.pojo.dto;

import com.qinghuan.pojo.enums.TicketTypeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 运营者修改票种资料时提交的字段。
 */
@Getter
@Setter
@Schema(description = "新建或修改景点基础票种的参数")
public class TicketTypeUpdateDTO {

    @Schema(description = "票种名称", example = "成人票")
    @NotBlank(message = "票种名称不能为空")
    @Size(max = 100, message = "票种名称不能超过100个字符")
    private String name;

    @Schema(description = "票种展示描述")
    @Size(max = 500, message = "票种描述不能超过500个字符")
    private String description;

    @Schema(description = "适用人群或证件规则", example = "18 周岁及以上游客")
    @Size(max = 500, message = "适用规则不能超过500个字符")
    private String audienceRule;

    @Schema(description = "票种基础价格；具体场次可配置独立售价", example = "50.00")
    @NotNull(message = "基础价格不能为空")
    @DecimalMin(value = "0.00", message = "基础价格不能小于0")
    @Digits(integer = 8, fraction = 2, message = "基础价格最多为8位整数和2位小数")
    private BigDecimal basePrice;

    @Schema(description = "票种状态", example = "ENABLED")
    @NotNull(message = "票种状态不能为空")
    private TicketTypeStatus status;
}
