package com.qinghuan.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 运营者重置工作人员密码的请求。 */
public record StaffPasswordResetDTO(
        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 50, message = "密码长度必须在6到50个字符之间")
        String password) {
}
