package com.qinghuan.config;

import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.qinghuan.booking.CouponClient;
import com.qinghuan.booking.VenueClient;
import feign.Target;
import org.springframework.cloud.openfeign.CircuitBreakerNameResolver;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.List;

/** 只为订单依赖的场次、优惠券服务注册异常比例熔断规则。 */
@Configuration
@EnableConfigurationProperties(FeignSentinelProperties.class)
public class FeignSentinelConfig {

    static final String VENUE_RESOURCE = "VenueClient";
    static final String COUPON_RESOURCE = "CouponClient";

    private final FeignSentinelProperties properties;

    public FeignSentinelConfig(FeignSentinelProperties properties) {
        this.properties = properties;
    }

    /**
     * 固定两类客户端的资源名，使 Sentinel 规则按服务维度生效；其他 Feign 客户端不配置降级规则。
     */
    @Bean
    CircuitBreakerNameResolver circuitBreakerNameResolver() {
        return (feignClientName, target, method) -> resourceName(target, feignClientName, method);
    }

    @PostConstruct
    void loadFeignDegradeRules() {
        DegradeRuleManager.loadRules(List.of(
                exceptionRatioRule(VENUE_RESOURCE, properties.getVenueExceptionRatio()),
                exceptionRatioRule(COUPON_RESOURCE, properties.getCouponExceptionRatio())
        ));
    }

    private static String resourceName(Target<?> target, String feignClientName, Method method) {
        if (target.type() == VenueClient.class) {
            return VENUE_RESOURCE;
        }
        if (target.type() == CouponClient.class) {
            return COUPON_RESOURCE;
        }
        // 仅维持其他客户端原有的按方法命名，不为它们注册 Sentinel 熔断规则。
        return feignClientName + "#" + method.getName();
    }

    private static DegradeRule exceptionRatioRule(String resource, double threshold) {
        DegradeRule rule = new DegradeRule(resource);
        rule.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO);
        rule.setCount(threshold);
        rule.setStatIntervalMs(10_000);
        rule.setMinRequestAmount(10);
        rule.setTimeWindow(10);
        return rule;
    }
}
