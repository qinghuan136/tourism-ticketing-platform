package com.qinghuan.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 单次票券核销请求。 */
@Schema(description = "单次电子票核销参数")
public record VerificationRequestDTO(
        @Schema(description = "客户端生成的核销请求号，用于防止重复提交", example = "VERIFY-20261001-0001")
        @NotBlank(message = "核销请求号不能为空")
        @Size(max = 64, message = "核销请求号不能超过 64 个字符")
        String requestNo,

        @Schema(description = "电子票唯一票码", example = "1b06717294c511f19b6474d4dd625c17")
        @NotBlank(message = "票码不能为空")
        @Size(max = 64, message = "票码不能超过 64 个字符")
        String ticketCode,

        @Schema(description = "核销设备编号，可不填写", example = "GATE-01")
        @Size(max = 64, message = "设备编号不能超过 64 个字符")
        String deviceNo
) {
}
