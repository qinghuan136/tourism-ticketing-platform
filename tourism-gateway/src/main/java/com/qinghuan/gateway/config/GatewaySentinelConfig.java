package com.qinghuan.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.Map;
import java.util.Set;

/**
 * Gateway 入口的静态 Sentinel 限流规则。
 */
@Configuration
@EnableConfigurationProperties(GatewaySentinelProperties.class)
public class GatewaySentinelConfig {

    static final String ORDER_CREATE_API = "order-create-api";
    static final String COUPON_CLAIM_API = "coupon-claim-api";
    static final String AUTH_LOGIN_API = "auth-login-api";

    private final GatewaySentinelProperties properties;

    public GatewaySentinelConfig(GatewaySentinelProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void initGatewaySentinelRules() {
        GatewayApiDefinitionManager.loadApiDefinitions(Set.of(
                apiDefinition(ORDER_CREATE_API, "/tourist/orders",
                        SentinelGatewayConstants.URL_MATCH_STRATEGY_EXACT),
                apiDefinition(COUPON_CLAIM_API, "/tourist/coupon-activities/[^/]+/claims",
                        SentinelGatewayConstants.URL_MATCH_STRATEGY_REGEX),
                apiDefinition(AUTH_LOGIN_API, "/auth/login",
                        SentinelGatewayConstants.URL_MATCH_STRATEGY_EXACT)
        ));

        GatewayRuleManager.loadRules(Set.of(
                qpsRule(ORDER_CREATE_API, properties.getOrderCreateQps()),
                qpsRule(COUPON_CLAIM_API, properties.getCouponClaimQps()),
                qpsRule(AUTH_LOGIN_API, properties.getAuthLoginQps())
        ));

        // 统一限流响应，避免向调用方暴露 Sentinel 默认文案。
        GatewayCallbackManager.setBlockHandler((exchange, cause) -> ServerResponse
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("code", 429, "message", "请求过于频繁，请稍后重试")));
    }

    private ApiDefinition apiDefinition(String apiName, String path, int matchStrategy) {
        ApiPathPredicateItem pathPredicate = new ApiPathPredicateItem()
                .setPattern(path)
                .setMatchStrategy(matchStrategy);
        return new ApiDefinition(apiName)
                .setPredicateItems(Set.<ApiPredicateItem>of(pathPredicate));
    }

    private GatewayFlowRule qpsRule(String resource, double qps) {
        return new GatewayFlowRule(resource)
                .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME)
                .setGrade(RuleConstant.FLOW_GRADE_QPS)
                .setCount(qps)
                .setIntervalSec(1);
    }
}
