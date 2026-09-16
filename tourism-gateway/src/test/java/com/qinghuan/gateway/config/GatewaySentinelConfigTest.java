package com.qinghuan.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.SentinelGatewayFilter;
import com.alibaba.csp.sentinel.adapter.gateway.sc.api.GatewayApiMatcherManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.exception.SentinelGatewayBlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewaySentinelConfigTest {

    @AfterEach
    void resetGatewayRules() {
        GatewayApiDefinitionManager.loadApiDefinitions(java.util.Set.of());
        GatewayRuleManager.loadRules(java.util.Set.of());
        GatewayCallbackManager.resetBlockHandler();
    }

    @Test
    void shouldRegisterExpectedApiGroupsAndQpsRules() {
        new GatewaySentinelConfig(new GatewaySentinelProperties()).initGatewaySentinelRules();

        assertEquals("/tourist/orders", pathOf(GatewaySentinelConfig.ORDER_CREATE_API));
        assertEquals("/tourist/coupon-activities/[^/]+/claims", pathOf(GatewaySentinelConfig.COUPON_CLAIM_API));
        assertEquals("/auth/login", pathOf(GatewaySentinelConfig.AUTH_LOGIN_API));
        assertEquals(SentinelGatewayConstants.URL_MATCH_STRATEGY_EXACT,
                pathPredicateOf(GatewaySentinelConfig.ORDER_CREATE_API).getMatchStrategy());
        assertEquals(SentinelGatewayConstants.URL_MATCH_STRATEGY_REGEX,
                pathPredicateOf(GatewaySentinelConfig.COUPON_CLAIM_API).getMatchStrategy());

        Map<String, GatewayFlowRule> rules = GatewayRuleManager.getRules().stream()
                .collect(Collectors.toMap(GatewayFlowRule::getResource, rule -> rule));
        assertQpsRule(rules.get(GatewaySentinelConfig.ORDER_CREATE_API), 30);
        assertQpsRule(rules.get(GatewaySentinelConfig.COUPON_CLAIM_API), 100);
        assertQpsRule(rules.get(GatewaySentinelConfig.AUTH_LOGIN_API), 20);
        assertNotNull(GatewayCallbackManager.getBlockHandler());
    }

    @Test
    void shouldBlockExceededOrderCreateQpsAndRender429Response() {
        new GatewaySentinelConfig(new GatewaySentinelProperties()).initGatewaySentinelRules();
        SentinelGatewayFilter filter = new SentinelGatewayFilter();
        GatewayFilterChain chain = exchange -> Mono.empty();
        assertTrue(GatewayApiMatcherManager.getApiMatcherMap()
                .get(GatewaySentinelConfig.ORDER_CREATE_API)
                .test(orderCreateExchange()));

        MockServerWebExchange blockedExchange = null;
        BlockException blockException = null;
        for (int i = 0; i < 100; i++) {
            MockServerWebExchange exchange = orderCreateExchange();
            try {
                filter.filter(exchange, chain).block();
            } catch (RuntimeException exception) {
                if (exception.getCause() instanceof BlockException cause) {
                    blockedExchange = exchange;
                    blockException = cause;
                    break;
                }
            }
        }

        assertNotNull(blockException);
        new SentinelGatewayBlockExceptionHandler(java.util.List.of(), ServerCodecConfigurer.create())
                .handle(blockedExchange, blockException)
                .block();

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, blockedExchange.getResponse().getStatusCode());
        assertTrue(blockedExchange.getResponse().getBodyAsString().block().contains("请求过于频繁"));
    }

    private void assertQpsRule(GatewayFlowRule rule, double qps) {
        assertEquals(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME, rule.getResourceMode());
        assertEquals(RuleConstant.FLOW_GRADE_QPS, rule.getGrade());
        assertEquals(qps, rule.getCount());
        assertEquals(1, rule.getIntervalSec());
    }

    private String pathOf(String apiName) {
        return pathPredicateOf(apiName).getPattern();
    }

    private ApiPathPredicateItem pathPredicateOf(String apiName) {
        return (ApiPathPredicateItem) GatewayApiDefinitionManager.getApiDefinition(apiName)
                .getPredicateItems().iterator().next();
    }

    private MockServerWebExchange orderCreateExchange() {
        return MockServerWebExchange.from(MockServerHttpRequest.post("/tourist/orders").build());
    }
}
