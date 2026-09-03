package com.qinghuan.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 游客自主注册参数。 */
@Schema(description = "游客自主注册参数")
public record RegisterDTO(
        @Schema(description = "登录名，注册后不可重复", example = "tourist_zhang")
        @NotBlank(message = "登录名不能为空")
        @Size(min = 4, max = 50, message = "登录名长度应为4到50个字符")
        String loginName,

        @Schema(description = "登录密码", example = "123456", format = "password")
        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 32, message = "密码长度应为6到32个字符")
        String password,

        @Schema(description = "游客昵称", example = "张先生")
        @NotBlank(message = "昵称不能为空")
        @Size(max = 50, message = "昵称不能超过50个字符")
        String displayName,

        @Schema(description = "手机号，可不填写", example = "13800138000")
        @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
        String phone
) {
}
