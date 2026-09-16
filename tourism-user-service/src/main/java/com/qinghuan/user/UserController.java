package com.qinghuan.user;

import com.qinghuan.annotation.RequireRole;
import com.qinghuan.common.response.ApiResponse;
import com.qinghuan.pojo.dto.StaffAccountUpdateDTO;
import com.qinghuan.pojo.dto.StaffAccountCreateDTO;
import com.qinghuan.pojo.dto.StaffPasswordResetDTO;
import com.qinghuan.pojo.dto.UserAccountPageQueryDTO;
import com.qinghuan.pojo.entity.UserAccount;
import com.qinghuan.pojo.enums.AccountRole;
import com.qinghuan.pojo.enums.AccountStatus;
import com.qinghuan.pojo.vo.PageResult;
import com.qinghuan.pojo.vo.UserAccountVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.BeanUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@Tag(name = "工作人员管理", description = "运营者维护所属景点的工作人员账号")
@SecurityRequirement(name = "BearerAuth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 保存工作人员账号
     */
    @RequireRole(AccountRole.OPERATOR)
    @PostMapping("/operator/staff")
    @Operation(summary = "保存工作人员账号")
    public ApiResponse<Void> saveStaffAccount(
            @Valid @RequestBody StaffAccountCreateDTO userAccountDTO) {
        UserAccount staffAccount = new UserAccount();
        BeanUtils.copyProperties(userAccountDTO, staffAccount);
        userService.saveStaffAccount(staffAccount);
        return ApiResponse.success();
    }

    // 获取工作人员账号分页
    @RequireRole(AccountRole.OPERATOR)
    @GetMapping("/operator/staff/page")
    @Operation(summary = "获取工作人员账号分页")
    public ApiResponse<PageResult<UserAccountVO>> StaffAccountPageQuery(
            @ParameterObject @Valid UserAccountPageQueryDTO userAccountPageQueryDTO) {
        PageResult<UserAccountVO> pageResult = userService.StaffAccountPageQuery(userAccountPageQueryDTO);
        return ApiResponse.success(pageResult);
    }

    // 修改工作人员账号状态
    @RequireRole(AccountRole.OPERATOR)
    @PostMapping("/operator/staff/status/{status}")
    @Operation(summary = "修改工作人员账号状态")
    public ApiResponse<Void> changeStaffAccountStatus(
            @PathVariable AccountStatus status,
            @RequestParam @Positive Long id) {
        userService.changeStaffAccountStatus(status, id);
        return ApiResponse.success();
    }

    /**
     * 仅允许运营者维护本景点工作人员的姓名和手机号。
     */
    @RequireRole(AccountRole.OPERATOR)
    @PutMapping("/operator/staff/{staffId}")
    @Operation(summary = "修改本景点工作人员的姓名和手机号")
    public ApiResponse<Void> updateStaffAccount(
            @PathVariable @Positive(message = "工作人员ID必须为正数") Long staffId,
            @Valid @RequestBody StaffAccountUpdateDTO updateDTO) {
        userService.updateStaffAccount(staffId, updateDTO);
        return ApiResponse.success();
    }

    /** 运营者为本景点工作人员重置登录密码。 */
    @RequireRole(AccountRole.OPERATOR)
    @PutMapping("/operator/staff/{staffId}/password")
    @Operation(summary = "重置工作人员密码")
    public ApiResponse<Void> resetStaffPassword(
            @PathVariable @Positive Long staffId,
            @Valid @RequestBody StaffPasswordResetDTO resetDTO) {
        userService.resetStaffPassword(staffId, resetDTO.password());
        return ApiResponse.success();
    }
}
