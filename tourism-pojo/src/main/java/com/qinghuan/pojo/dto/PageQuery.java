package com.qinghuan.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

/**
 * 通用分页查询参数，页码从 1 开始。
 */
@Getter
@Setter
@Schema(description = "通用分页参数，页码从 1 开始")
public class PageQuery {

    @Schema(description = "页码，从 1 开始", example = "1", defaultValue = "1")
    @Min(value = 1, message = "页码不能小于 1")
    private int page = 1;

    @Schema(description = "每页数量，范围 1 到 100", example = "20", defaultValue = "20")
    @Min(value = 1, message = "每页数量不能小于 1")
    @Max(value = 100, message = "每页数量不能超过 100")
    private int size = 20;

    public int offset() {
        return (page - 1) * size;
    }
}
