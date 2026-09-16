package com.qinghuan.aspect;

import com.qinghuan.annotation.RequireRole;
import com.qinghuan.auth.context.UserContext;
import com.qinghuan.auth.model.LoginUser;
import com.qinghuan.common.exception.BusinessException;
import com.qinghuan.pojo.enums.AccountRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("角色权限切面")
class RolePermissionAspectTest {

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    @DisplayName("只允许注解声明的角色调用方法")
    void checkPermission_shouldUseRolesDeclaredByAnnotation() {
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new OperatorService());
        proxyFactory.addAspect(new RolePermissionAspect());
        OperatorService service = proxyFactory.getProxy();

        UserContext.set(new LoginUser(1L, "operator", AccountRole.OPERATOR, 10L));
        assertDoesNotThrow(service::execute);

        UserContext.set(new LoginUser(2L, "staff", AccountRole.STAFF, 10L));
        assertThrows(BusinessException.class, service::execute);
    }

    static class OperatorService {

        @RequireRole(AccountRole.OPERATOR)
        public void execute() {
        }
    }
}
