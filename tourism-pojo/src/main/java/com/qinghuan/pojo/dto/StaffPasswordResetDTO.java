package com.qinghuan.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 运营者重置工作人员密码的请求。 */
@Schema(description = "运营者重置工作人员密码的参数")
public record StaffPasswordResetDTO(
        @Schema(description = "新的登录密码", example = "123456", format = "password")
        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 50, message = "密码长度必须在6到50个字符之间")
        String password) {
}
