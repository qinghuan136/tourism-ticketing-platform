package com.qinghuan.aspect;

import com.qinghuan.annotation.RequireRole;
import com.qinghuan.auth.context.UserContext;
import com.qinghuan.auth.model.LoginUser;
import com.qinghuan.common.exception.BusinessException;
import com.qinghuan.common.exception.ErrorCode;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.util.Arrays;

// 权限切面,判断角色能否进入接口
@Component
@Aspect
public class RolePermissionAspect {

    // 权限检查
    @Before("@annotation(requireRole)")
    public void checkPermission(RequireRole requireRole) {
        LoginUser loginUser = UserContext.getRequired();
        if (Arrays.stream(requireRole.value()).anyMatch(role -> role == loginUser.roleCode())) {
            return;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN);
    }
}
