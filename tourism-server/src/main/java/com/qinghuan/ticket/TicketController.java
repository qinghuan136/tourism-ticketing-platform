package com.qinghuan.ticket;

import com.qinghuan.annotation.RequireRole;
import com.qinghuan.common.response.ApiResponse;
import com.qinghuan.pojo.dto.TicketPageQueryDTO;
import com.qinghuan.pojo.enums.AccountRole;
import com.qinghuan.pojo.vo.PageResult;
import com.qinghuan.pojo.vo.TicketVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@Tag(name = "电子票", description = "游客本人电子票列表与详情查询")
@SecurityRequirement(name = "BearerAuth")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/tourist/tickets")
    @RequireRole(AccountRole.TOURIST)
    @Operation(summary = "分页查询我的票券")
    public ApiResponse<PageResult<TicketVO>> pageMyTickets(
            @ParameterObject @Valid TicketPageQueryDTO queryDTO) {
        return ApiResponse.success(ticketService.pageMyTickets(queryDTO));
    }

    @GetMapping("/tourist/tickets/{ticketId}")
    @RequireRole(AccountRole.TOURIST)
    @Operation(summary = "获取我的票券详情")
    public ApiResponse<TicketVO> getMyTicket(
            @PathVariable @Positive(message = "票券ID必须为正数") Long ticketId) {
        return ApiResponse.success(ticketService.getMyTicket(ticketId));
    }
}
