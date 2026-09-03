package com.qinghuan.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.Size;

@Getter
@Setter
public class TicketTypePageQueryDTO extends PageQuery {

    @Schema(description = "按票种名称模糊搜索", example = "成人")
    @Size(max = 100, message = "查询关键字不能超过100个字符")
    private String keyword;
}
