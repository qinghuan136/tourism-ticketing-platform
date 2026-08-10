package com.qinghuan.pojo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** 运营统计日期范围，结束日期按自然日完整包含。 */
@Getter
@Setter
public class StatisticsDateRangeDTO {

    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;
}
