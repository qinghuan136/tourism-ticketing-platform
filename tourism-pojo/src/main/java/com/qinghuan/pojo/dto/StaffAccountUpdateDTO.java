package com.qinghuan.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 运营者修改工作人员基本资料的请求参数。
 * 登录账号、密码、状态和所属景点由各自的业务接口处理。
 */
@Getter
@Setter
@Schema(description = "修改工作人员姓名和手机号的参数")
public class StaffAccountUpdateDTO {

    @Schema(description = "工作人员姓名", example = "张伟")
    @NotBlank(message = "工作人员姓名不能为空")
    @Size(max = 50, message = "工作人员姓名不能超过50个字符")
    private String displayName;

    @Schema(description = "工作人员手机号", example = "13800138000")
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String phone;
}
