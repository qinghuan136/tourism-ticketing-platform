package com.qinghuan.tickettype;

import com.qinghuan.annotation.RequireRole;
import com.qinghuan.common.response.ApiResponse;
import com.qinghuan.pojo.dto.TicketTypePageQueryDTO;
import com.qinghuan.pojo.dto.TicketTypeUpdateDTO;
import com.qinghuan.pojo.entity.TicketType;
import com.qinghuan.pojo.enums.AccountRole;
import com.qinghuan.pojo.vo.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@Validated
@Tag(name = "票种管理", description = "运营者维护所属景点可复用的基础票种")
@SecurityRequirement(name = "BearerAuth")
public class TicketTypeController {

    private final TicketTypeService ticketTypeService;

    public TicketTypeController(TicketTypeService ticketTypeService) {
        this.ticketTypeService = ticketTypeService;
    }

    /*
     * 分页查询票种
     */
    @RequireRole(AccountRole.OPERATOR)
    @GetMapping("/operator/ticket-types")
    @Operation(summary = "分页查询票种")
    public ApiResponse<PageResult<TicketType>> pageQueryTicketType(
            @ParameterObject @Valid TicketTypePageQueryDTO ticketTypePageQueryDTO) {
        log.info("分页查询票种: {}", ticketTypePageQueryDTO);
        PageResult<TicketType> pageResult = ticketTypeService.pageQueryTicketType(ticketTypePageQueryDTO);
        return ApiResponse.success(pageResult);
    }

    @Operation(summary = "获取票种详情")
    @GetMapping("/operator/ticket-types/{ticketTypeId}")
    @RequireRole(AccountRole.OPERATOR)
    public ApiResponse<TicketType> getTicketTypeById(
            @PathVariable @Positive Long ticketTypeId) {
        log.info("获取票种详情: {}", ticketTypeId);
        TicketType ticketType = ticketTypeService.getTicketTypeById(ticketTypeId);
        return ApiResponse.success(ticketType);
    }

    @Operation(summary = "新建票种")
    @PostMapping("/operator/ticket-types")
    @RequireRole(AccountRole.OPERATOR)
    public ApiResponse<Integer> createTicketType(
            @Valid @RequestBody TicketTypeUpdateDTO ticketType) {
        log.info("新建票种: {}", ticketType);
        int result = ticketTypeService.createTicketType(ticketType);
        return ApiResponse.success(result);
    }

    @Operation(summary = "修改票种")
    @PutMapping("/operator/ticket-types/{ticketTypeId}")
    @RequireRole(AccountRole.OPERATOR)
    public ApiResponse<Void> updateTicketType(
            @PathVariable Long ticketTypeId,
            @Valid @RequestBody TicketTypeUpdateDTO updateDTO) {
        log.info("修改票种: id={}", ticketTypeId);
        ticketTypeService.updateTicketType(ticketTypeId, updateDTO);
        return ApiResponse.success();
    }

    @Operation(summary = "批量删除票种")
    @DeleteMapping("/operator/ticket-types")
    @RequireRole(AccountRole.OPERATOR)
    public ApiResponse<Void> deleteTicketTypes(
            @RequestParam("ids")
            @Size(min = 1, max = 100, message = "票种ID数量必须在1到100之间")
            List<Long> ids) {
        log.info("批量删除票种: {}", ids);
        ticketTypeService.deleteBatch(ids);
        return ApiResponse.success();
    }
}
