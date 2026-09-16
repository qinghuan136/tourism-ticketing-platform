package com.qinghuan.aspect;

import com.qinghuan.annotation.RefreshCreateTimeOrUpdateTime;
import com.qinghuan.pojo.entity.BaseEntity;
import com.qinghuan.pojo.enums.OperationType;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/*
 * 自动填充字段切面
 */
@Component
@Aspect
public class AutoFillAspect {

    /*
     * 自动填充创建时间或修改时间
     */
    @Before("@annotation(refreshTime)")
    public void autoFillCreateTimeOrUpdateTime(
            JoinPoint joinPoint,
            RefreshCreateTimeOrUpdateTime refreshTime) {
        Object[] args = joinPoint.getArgs();
        if (args.length == 0 || !(args[0] instanceof BaseEntity entity)) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        if (refreshTime.value() == OperationType.INSERT) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedAt(now);
    }
}
