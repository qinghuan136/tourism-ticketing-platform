package com.qinghuan.auth;

import com.qinghuan.common.response.ApiResponse;
import com.qinghuan.pojo.dto.RegisterDTO;
import com.qinghuan.pojo.dto.UserAccountDTO;
import com.qinghuan.pojo.entity.UserAccount;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/auth")
@Tag(name = "认证", description = "游客注册与账号登录")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 游客自主注册；角色和账号状态由后端固定设置。 */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "游客注册", description = "创建游客账号；账号角色和初始状态由后端设置")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterDTO registerDTO) {
        authService.register(registerDTO);
        return ApiResponse.success();
    }

    /** 使用登录名和密码换取 JWT。 */
    @PostMapping("/login")
    @Operation(summary = "账号登录", description = "使用登录名和密码换取 JWT，后续受保护接口使用 Bearer Token")
    public ApiResponse<String> login(@Valid @RequestBody UserAccountDTO userAccountDTO) {
        log.info("登录账号: {}", userAccountDTO.getLoginName());
        UserAccount userAccount = new UserAccount();
        BeanUtils.copyProperties(userAccountDTO, userAccount);
        return ApiResponse.success(authService.login(userAccount));
    }
}
