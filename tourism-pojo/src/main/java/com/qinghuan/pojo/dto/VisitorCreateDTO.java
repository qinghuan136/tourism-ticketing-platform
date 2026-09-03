package com.qinghuan.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 游客新建参观人的请求参数。
 */
@Getter
@Setter
@Schema(description = "游客新建常用参观人的参数")
public class VisitorCreateDTO {

    @Schema(description = "参观人姓名", example = "李明")
    @NotBlank(message = "参观人姓名不能为空")
    @Size(max = 50, message = "参观人姓名不能超过50个字符")
    private String name;

    @Schema(description = "证件类型", example = "ID_CARD")
    @NotBlank(message = "证件类型不能为空")
    @Size(max = 20, message = "证件类型不能超过20个字符")
    private String idType;

    @Schema(description = "证件号码", example = "440101199803120011")
    @NotBlank(message = "证件号码不能为空")
    @Size(max = 64, message = "证件号码不能超过64个字符")
    private String idNumber;

    @Schema(description = "参观人手机号，可不填写", example = "13800138000")
    @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号格式不正确")
    private String phone;
}
