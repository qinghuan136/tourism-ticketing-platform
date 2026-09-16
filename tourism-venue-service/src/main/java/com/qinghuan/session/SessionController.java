package com.qinghuan.session;

import com.qinghuan.annotation.RequireRole;
import com.qinghuan.common.response.ApiResponse;
import com.qinghuan.pojo.dto.SessionEventDTO;
import com.qinghuan.pojo.dto.SessionPageQueryDTO;
import com.qinghuan.pojo.dto.SessionWriteDTO;
import com.qinghuan.pojo.enums.AccountRole;
import com.qinghuan.pojo.vo.PageResult;
import com.qinghuan.pojo.vo.SessionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("/operator/sessions")
@Tag(name = "场次管理", description = "运营者维护场次、票种配额并通过事件驱动状态流转")
@SecurityRequirement(name = "BearerAuth")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    /**
     * 分页查询当前运营者所属景点的场次。
     */
    @GetMapping
    @RequireRole(AccountRole.OPERATOR)
    @Operation(summary = "分页查询场次")
    public ApiResponse<PageResult<SessionVO>> pageSessions(
            @ParameterObject @Valid SessionPageQueryDTO queryDTO) {
        return ApiResponse.success(sessionService.pageSessions(queryDTO));
    }

    /**
     * 创建草稿场次，并保存本场次的票种配额配置。
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequireRole(AccountRole.OPERATOR)
    @Operation(summary = "新建场次")
    public ApiResponse<Long> createSession(@Valid @RequestBody SessionWriteDTO writeDTO) {
        Long sessionId = sessionService.createSession(writeDTO);
        log.info("新建场次成功: id={}", sessionId);
        return ApiResponse.success(sessionId);
    }

    /**
     * 获取当前景点内场次的完整资料和票种配置。
     */
    @GetMapping("/{sessionId}")
    @RequireRole(AccountRole.OPERATOR)
    @Operation(summary = "获取场次详情")
    public ApiResponse<SessionVO> getSession(
            @PathVariable @Positive(message = "场次ID必须为正数") Long sessionId) {
        return ApiResponse.success(sessionService.getSession(sessionId));
    }

    /**
     * 仅允许修改草稿场次，提交的票种配置会整组替换。
     */
    @PutMapping("/{sessionId}")
    @RequireRole(AccountRole.OPERATOR)
    @Operation(summary = "修改草稿场次")
    public ApiResponse<Void> updateDraftSession(
            @PathVariable @Positive(message = "场次ID必须为正数") Long sessionId,
            @Valid @RequestBody SessionWriteDTO writeDTO) {
        sessionService.updateDraftSession(sessionId, writeDTO);
        return ApiResponse.success();
    }

    /**
     * 提交业务事件，由后端状态机计算下一状态。
     */
    @PostMapping("/{sessionId}/events")
    @RequireRole(AccountRole.OPERATOR)
    @Operation(summary = "触发场次事件")
    public ApiResponse<Void> handleSessionEvent(
            @PathVariable @Positive(message = "场次ID必须为正数") Long sessionId,
            @Valid @RequestBody SessionEventDTO eventDTO) {
        sessionService.handleSessionEvent(sessionId, eventDTO.getEvent());
        return ApiResponse.success();
    }

    /**
     * 删除没有订单的草稿场次。
     */
    @DeleteMapping("/{sessionId}")
    @RequireRole(AccountRole.OPERATOR)
    @Operation(summary = "删除草稿场次")
    public ApiResponse<Void> deleteDraftSession(
            @PathVariable @Positive(message = "场次ID必须为正数") Long sessionId) {
        sessionService.deleteDraftSession(sessionId);
        return ApiResponse.success();
    }
}
