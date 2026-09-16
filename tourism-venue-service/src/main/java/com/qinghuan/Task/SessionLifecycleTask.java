package com.qinghuan.Task;

import com.qinghuan.session.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** 场次状态由场次服务自行定时收口，避免订单服务依赖本地场次实现。 */
@Slf4j
@Component
public class SessionLifecycleTask {

    private final SessionService sessionService;

    public SessionLifecycleTask(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Scheduled(cron = "30 * * * * *")
    public void maintainLifecycle() {
        int sessions = sessionService.maintainLifecycle(LocalDateTime.now());
        if (sessions > 0) {
            log.info("场次状态收口完成：{}", sessions);
        }
    }
}
