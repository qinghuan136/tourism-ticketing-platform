package com.qinghuan.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** 运营统计日期范围，结束日期按自然日完整包含。 */
@Getter
@Setter
public class StatisticsDateRangeDTO {

    @Schema(description = "统计开始日期，包含当天", example = "2026-10-01")
    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    @Schema(description = "统计结束日期，包含当天", example = "2026-10-31")
    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;
}
