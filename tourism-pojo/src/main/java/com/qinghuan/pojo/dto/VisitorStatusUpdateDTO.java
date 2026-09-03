package com.qinghuan.pojo.dto;

import com.qinghuan.pojo.enums.VisitorStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 启用或停用参观人的请求参数。
 */
@Getter
@Setter
@Schema(description = "启用或停用参观人的参数")
public class VisitorStatusUpdateDTO {

    @Schema(description = "参观人状态", example = "ENABLED")
    @NotNull(message = "参观人状态不能为空")
    private VisitorStatus status;
}
