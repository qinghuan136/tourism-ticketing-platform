package com.qinghuan.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 修改参观人基础资料的请求参数，证件信息不在此接口中变更。
 */
@Getter
@Setter
@Schema(description = "修改参观人姓名和手机号的参数；证件信息不允许修改")
public class VisitorUpdateDTO {

    @Schema(description = "参观人姓名", example = "李明")
    @NotBlank(message = "参观人姓名不能为空")
    @Size(max = 50, message = "参观人姓名不能超过50个字符")
    private String name;

    @Schema(description = "参观人手机号，可不填写", example = "13800138000")
    @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号格式不正确")
    private String phone;
}
