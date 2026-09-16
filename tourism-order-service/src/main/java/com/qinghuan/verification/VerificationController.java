package com.qinghuan.verification;

import com.qinghuan.annotation.RequireRole;
import com.qinghuan.common.response.ApiResponse;
import com.qinghuan.pojo.dto.VerificationPageQueryDTO;
import com.qinghuan.pojo.dto.VerificationRequestDTO;
import com.qinghuan.pojo.enums.AccountRole;
import com.qinghuan.pojo.vo.PageResult;
import com.qinghuan.pojo.vo.VerificationRecordVO;
import com.qinghuan.pojo.vo.VerificationResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "票券核销", description = "工作人员或运营者核销票券，运营者查询核销记录")
@SecurityRequirement(name = "BearerAuth")
public class VerificationController {

    private final VerificationService verificationService;

    public VerificationController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @PostMapping("/operator/verifications")
    @RequireRole({AccountRole.OPERATOR, AccountRole.STAFF})
    @Operation(summary = "提交核销请求")
    public ApiResponse<VerificationResultVO> verify(
            @Valid @RequestBody VerificationRequestDTO requestDTO) {
        return ApiResponse.success(verificationService.verify(requestDTO));
    }

    @GetMapping("/operator/verifications")
    @RequireRole(AccountRole.OPERATOR)
    @Operation(summary = "分页查询核销记录")
    public ApiResponse<PageResult<VerificationRecordVO>> pageRecords(
            @ParameterObject @Valid VerificationPageQueryDTO queryDTO) {
        return ApiResponse.success(verificationService.pageRecords(queryDTO));
    }
}
