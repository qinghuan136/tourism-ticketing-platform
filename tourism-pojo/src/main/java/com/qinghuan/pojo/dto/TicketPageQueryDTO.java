package com.qinghuan.pojo.dto;

import com.qinghuan.pojo.enums.TicketStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/** 游客端电子票分页筛选条件。 */
@Getter
@Setter
public class TicketPageQueryDTO extends PageQuery {

    @Schema(description = "电子票状态筛选，不传则查询全部状态")
    private TicketStatus status;
}
