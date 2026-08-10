package com.qinghuan.pojo.dto;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.Size;

@Getter
@Setter
public class TicketTypePageQueryDTO extends PageQuery {

    @Size(max = 100, message = "查询关键字不能超过100个字符")
    private String keyword;
}
