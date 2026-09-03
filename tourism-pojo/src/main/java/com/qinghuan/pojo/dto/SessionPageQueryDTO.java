package com.qinghuan.pojo.dto;

import com.qinghuan.pojo.enums.AdmissionSessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 运营端场次分页筛选条件。
 */
@Getter
@Setter
public class SessionPageQueryDTO extends PageQuery {

    @Schema(description = "参观日期筛选", example = "2026-10-01")
    private LocalDate visitDate;
    @Schema(description = "场次状态筛选")
    private AdmissionSessionStatus status;
}
