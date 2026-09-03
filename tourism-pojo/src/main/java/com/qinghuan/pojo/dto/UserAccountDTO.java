package com.qinghuan.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Setter
@Getter
@Schema(description = "账号登录参数")
public class UserAccountDTO {
    @Schema(description = "登录名", example = "tourist_demo")
    @NotBlank(message = "登录名不能为空")
    @Size(max = 50, message = "登录名不能超过50个字符")
    private String loginName;

    @Schema(description = "登录密码", example = "123456", format = "password")
    @NotBlank(message = "密码不能为空")
    @Size(max = 50, message = "密码不能超过50个字符")
    private String password;
}
