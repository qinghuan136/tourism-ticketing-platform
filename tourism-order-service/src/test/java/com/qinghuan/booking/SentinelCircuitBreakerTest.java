package com.qinghuan.booking;

import com.alibaba.cloud.circuitbreaker.sentinel.SentinelCircuitBreaker;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SentinelCircuitBreakerTest {

    private static final String RESOURCE = "sentinel-circuit-breaker-test";

    @AfterEach
    void clearRules() {
        DegradeRuleManager.loadRules(null);
    }

    @Test
    void shouldFastFailAfterExceptionAndRecoverAfterTimeWindow() throws InterruptedException {
        DegradeRule rule = new DegradeRule(RESOURCE);
        rule.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_COUNT);
        rule.setCount(1);
        rule.setStatIntervalMs(1000);
        rule.setMinRequestAmount(1);
        // 使用 1 秒窗口缩短单元测试；生产规则仍为 15 秒。
        rule.setTimeWindow(1);
        DegradeRuleManager.loadRules(List.of(rule));
        SentinelCircuitBreaker circuitBreaker = new SentinelCircuitBreaker(RESOURCE);
        AtomicInteger calls = new AtomicInteger();

        assertEquals("fallback", circuitBreaker.run(() -> {
            calls.incrementAndGet();
            throw new IllegalStateException("downstream unavailable");
        }, cause -> "fallback"));
        assertEquals("fallback", circuitBreaker.run(() -> {
            calls.incrementAndGet();
            throw new IllegalStateException("downstream unavailable again");
        }, cause -> "fallback"));
        assertEquals("fallback", circuitBreaker.run(() -> {
            calls.incrementAndGet();
            return "should not run while open";
        }, cause -> "fallback"));
        assertEquals(2, calls.get());

        Thread.sleep(1_100);
        assertEquals("recovered", circuitBreaker.run(() -> {
            calls.incrementAndGet();
            return "recovered";
        }, cause -> "fallback"));
        assertEquals(3, calls.get());
    }
}
