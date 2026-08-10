package com.qinghuan.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 游客自主注册参数。 */
public record RegisterDTO(
        @NotBlank(message = "登录名不能为空")
        @Size(min = 4, max = 50, message = "登录名长度应为4到50个字符")
        String loginName,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 32, message = "密码长度应为6到32个字符")
        String password,

        @NotBlank(message = "昵称不能为空")
        @Size(max = 50, message = "昵称不能超过50个字符")
        String displayName,

        @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
        String phone
) {
}
