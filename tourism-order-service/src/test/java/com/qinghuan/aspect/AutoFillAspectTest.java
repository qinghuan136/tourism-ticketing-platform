package com.qinghuan.aspect;

import com.qinghuan.annotation.RefreshCreateTimeOrUpdateTime;
import com.qinghuan.pojo.entity.UserAccount;
import com.qinghuan.pojo.enums.OperationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("审计时间自动填充")
class AutoFillAspectTest {

    @Test
    @DisplayName("插入实体前应填充创建和更新时间")
    void autoFill_shouldSetAuditTimes_forInsertEntity() {
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new AuditService());
        proxyFactory.addAspect(new AutoFillAspect());
        AuditService service = proxyFactory.getProxy();
        UserAccount account = new UserAccount();

        service.insert(account);

        assertNotNull(account.getCreatedAt());
        assertNotNull(account.getUpdatedAt());
    }

    static class AuditService {

        @RefreshCreateTimeOrUpdateTime(OperationType.INSERT)
        public void insert(UserAccount account) {
        }
    }
}
