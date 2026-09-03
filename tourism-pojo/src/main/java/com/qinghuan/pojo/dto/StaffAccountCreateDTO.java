package com.qinghuan.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** 运营者创建工作人员账号时提交的资料。 */
@Getter
@Setter
@Schema(description = "运营者创建所属景点工作人员账号的参数")
public class StaffAccountCreateDTO {

    @Schema(description = "工作人员登录名", example = "staff_zhang")
    @NotBlank(message = "登录名不能为空")
    @Size(max = 50, message = "登录名不能超过50个字符")
    private String loginName;

    @Schema(description = "初始登录密码", example = "123456", format = "password")
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度必须在6到50个字符之间")
    private String password;

    @Schema(description = "工作人员姓名", example = "张伟")
    @NotBlank(message = "工作人员姓名不能为空")
    @Size(max = 50, message = "工作人员姓名不能超过50个字符")
    private String displayName;

    @Schema(description = "工作人员手机号", example = "13800138000")
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String phone;
}
