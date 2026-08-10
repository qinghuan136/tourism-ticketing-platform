package com.qinghuan.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Setter
@Getter
public class UserAccountDTO {
    @NotBlank(message = "登录名不能为空")
    @Size(max = 50, message = "登录名不能超过50个字符")
    private String loginName;

    @NotBlank(message = "密码不能为空")
    @Size(max = 50, message = "密码不能超过50个字符")
    private String password;
}
