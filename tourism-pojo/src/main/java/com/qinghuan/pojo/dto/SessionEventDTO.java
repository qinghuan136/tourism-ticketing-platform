package com.qinghuan.pojo.dto;

import com.qinghuan.pojo.enums.SessionEvent;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 触发场次状态变化的业务事件。
 */
@Getter
@Setter
@Schema(description = "驱动场次状态转换的事件")
public class SessionEventDTO {

    @Schema(description = "场次事件；下一状态由当前状态和事件共同确定", example = "PUBLISH")
    @NotNull(message = "场次事件不能为空")
    private SessionEvent event;
}
