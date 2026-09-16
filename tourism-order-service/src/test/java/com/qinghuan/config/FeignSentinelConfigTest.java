package com.qinghuan.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.qinghuan.booking.CouponClient;
import com.qinghuan.booking.VenueClient;
import feign.Target;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.CircuitBreakerNameResolver;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FeignSentinelConfigTest {

    @AfterEach
    void clearRules() {
        DegradeRuleManager.loadRules(null);
    }

    @Test
    void shouldRegisterExceptionRatioRulesForVenueAndCouponOnly() {
        FeignSentinelConfig config = new FeignSentinelConfig(new FeignSentinelProperties());

        config.loadFeignDegradeRules();

        assertRule(FeignSentinelConfig.VENUE_RESOURCE, 0.5);
        assertRule(FeignSentinelConfig.COUPON_RESOURCE, 0.6);
        assertEquals(0, DegradeRuleManager.getRulesOfResource("UserClient").size());
    }

    @Test
    void shouldUseStableResourceNamesForProtectedFeignClients() throws NoSuchMethodException {
        CircuitBreakerNameResolver resolver = new FeignSentinelConfig(new FeignSentinelProperties())
                .circuitBreakerNameResolver();
        Method venueMethod = VenueClient.class.getMethod("tryReserve",
                com.qinghuan.pojo.remote.venue.InventoryTryReserveRequest.class);
        Method couponMethod = CouponClient.class.getMethod("tryLock",
                com.qinghuan.pojo.remote.coupon.CouponOrderTryRequest.class);

        assertEquals(FeignSentinelConfig.VENUE_RESOURCE, resolver.resolveCircuitBreakerName(
                "venue-service", new Target.HardCodedTarget<>(VenueClient.class, "http://venue-service"), venueMethod));
        assertEquals(FeignSentinelConfig.COUPON_RESOURCE, resolver.resolveCircuitBreakerName(
                "coupon-service", new Target.HardCodedTarget<>(CouponClient.class, "http://coupon-service"), couponMethod));
    }

    private void assertRule(String resource, double threshold) {
        DegradeRule rule = DegradeRuleManager.getRulesOfResource(resource).iterator().next();
        assertEquals(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO, rule.getGrade());
        assertEquals(threshold, rule.getCount());
        assertEquals(10_000, rule.getStatIntervalMs());
        assertEquals(10, rule.getMinRequestAmount());
        assertEquals(10, rule.getTimeWindow());
    }
}
