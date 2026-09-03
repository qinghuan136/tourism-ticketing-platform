package com.qinghuan.pojo.dto;

import com.qinghuan.pojo.enums.VerificationResult;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** 运营端核销记录分页筛选条件。 */
@Getter
@Setter
public class VerificationPageQueryDTO extends PageQuery {

    @Schema(description = "票码筛选", example = "1b06717294c511f19b6474d4dd625c17")
    @Size(max = 64, message = "票码不能超过 64 个字符")
    private String ticketCode;
    @Schema(description = "核销结果筛选")
    private VerificationResult result;
    @Schema(description = "核销日期筛选", example = "2026-10-01")
    private LocalDate verificationDate;
}
