package com.qinghuan.auth;

import com.qinghuan.auth.jwt.JwtUtils;
import com.qinghuan.auth.model.LoginUser;
import com.qinghuan.common.exception.BusinessException;
import com.qinghuan.common.exception.ErrorCode;
import com.qinghuan.pojo.dto.RegisterDTO;
import com.qinghuan.pojo.entity.UserAccount;
import com.qinghuan.pojo.enums.AccountStatus;
import com.qinghuan.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final JwtUtils jwtUtils;

    public AuthServiceImpl(UserService userService, JwtUtils jwtUtils) {
        this.userService = userService;
        this.jwtUtils = jwtUtils;
    }

    @Override
    public void register(RegisterDTO registerDTO) {
        // 账号落库属于用户模块，认证模块只负责注册流程入口。
        userService.registerTourist(registerDTO);
    }

    @Override
    public String login(UserAccount userAccount) {
        String loginName = userAccount.getLoginName();
        String passwordHash = DigestUtils.md5DigestAsHex(userAccount.getPassword().getBytes());

        UserAccount foundUserAccount = userService
                .getAccountByLoginNameandPasswordHash(loginName, passwordHash);
        if (foundUserAccount == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (foundUserAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已停用");
        }

        return jwtUtils.generateAccessToken(new LoginUser(
                foundUserAccount.getId(),
                foundUserAccount.getLoginName(),
                foundUserAccount.getRoleCode(),
                foundUserAccount.getVenueId()));
    }
}
