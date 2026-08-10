package com.qinghuan.auth;

import com.qinghuan.common.response.ApiResponse;
import com.qinghuan.pojo.dto.RegisterDTO;
import com.qinghuan.pojo.dto.UserAccountDTO;
import com.qinghuan.pojo.entity.UserAccount;
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
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 游客自主注册；角色和账号状态由后端固定设置。 */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> register(@Valid @RequestBody RegisterDTO registerDTO) {
        authService.register(registerDTO);
        return ApiResponse.success();
    }

    /** 使用登录名和密码换取 JWT。 */
    @PostMapping("/login")
    public ApiResponse<String> login(@Valid @RequestBody UserAccountDTO userAccountDTO) {
        log.info("登录账号: {}", userAccountDTO.getLoginName());
        UserAccount userAccount = new UserAccount();
        BeanUtils.copyProperties(userAccountDTO, userAccount);
        return ApiResponse.success(authService.login(userAccount));
    }
}
