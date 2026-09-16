package com.qinghuan.visitor;

import com.qinghuan.annotation.RequireRole;
import com.qinghuan.common.response.ApiResponse;
import com.qinghuan.pojo.dto.VisitorCreateDTO;
import com.qinghuan.pojo.dto.VisitorStatusUpdateDTO;
import com.qinghuan.pojo.dto.VisitorUpdateDTO;
import com.qinghuan.pojo.enums.AccountRole;
import com.qinghuan.pojo.enums.VisitorStatus;
import com.qinghuan.pojo.vo.VisitorVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/tourist/visitors")
@Tag(name = "参观人管理", description = "游客维护本人常用参观人")
@SecurityRequirement(name = "BearerAuth")
public class VisitorController {

    private final VisitorService visitorService;

    public VisitorController(VisitorService visitorService) {
        this.visitorService = visitorService;
    }

    /** 查询当前游客的全部参观人，可按状态筛选。 */
    @GetMapping
    @RequireRole(AccountRole.TOURIST)
    @Operation(summary = "查询参观人列表")
    public ApiResponse<List<VisitorVO>> listVisitors(
            @RequestParam(required = false) VisitorStatus status) {
        return ApiResponse.success(visitorService.listVisitors(status));
    }

    /** 新建参观人，所属游客和初始状态由后端确定。 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequireRole(AccountRole.TOURIST)
    @Operation(summary = "新建参观人")
    public ApiResponse<Long> createVisitor(
            @Valid @RequestBody VisitorCreateDTO createDTO) {
        return ApiResponse.success(visitorService.createVisitor(createDTO));
    }

    /** 获取当前游客名下的参观人详情。 */
    @GetMapping("/{visitorId}")
    @RequireRole(AccountRole.TOURIST)
    @Operation(summary = "获取参观人详情")
    public ApiResponse<VisitorVO> getVisitor(
            @PathVariable @Positive(message = "参观人ID必须为正数") Long visitorId) {
        return ApiResponse.success(visitorService.getVisitor(visitorId));
    }

    /** 修改姓名和手机号，证件信息保持不变。 */
    @PutMapping("/{visitorId}")
    @RequireRole(AccountRole.TOURIST)
    @Operation(summary = "修改参观人信息")
    public ApiResponse<Void> updateVisitor(
            @PathVariable @Positive(message = "参观人ID必须为正数") Long visitorId,
            @Valid @RequestBody VisitorUpdateDTO updateDTO) {
        visitorService.updateVisitor(visitorId, updateDTO);
        return ApiResponse.success();
    }

    /** 启用或停用参观人，不影响历史订单。 */
    @PatchMapping("/{visitorId}/status")
    @RequireRole(AccountRole.TOURIST)
    @Operation(summary = "修改参观人状态")
    public ApiResponse<Void> updateVisitorStatus(
            @PathVariable @Positive(message = "参观人ID必须为正数") Long visitorId,
            @Valid @RequestBody VisitorStatusUpdateDTO statusDTO) {
        visitorService.updateVisitorStatus(visitorId, statusDTO.getStatus());
        return ApiResponse.success();
    }
}
