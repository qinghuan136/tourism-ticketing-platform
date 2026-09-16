package com.qinghuan.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Gateway 限流阈值，启动时从 Nacos 配置绑定。 */
@ConfigurationProperties(prefix = "app.sentinel.gateway")
public class GatewaySentinelProperties {

    private double orderCreateQps = 30;
    private double couponClaimQps = 100;
    private double authLoginQps = 20;

    public double getOrderCreateQps() {
        return orderCreateQps;
    }

    public void setOrderCreateQps(double orderCreateQps) {
        this.orderCreateQps = orderCreateQps;
    }

    public double getCouponClaimQps() {
        return couponClaimQps;
    }

    public void setCouponClaimQps(double couponClaimQps) {
        this.couponClaimQps = couponClaimQps;
    }

    public double getAuthLoginQps() {
        return authLoginQps;
    }

    public void setAuthLoginQps(double authLoginQps) {
        this.authLoginQps = authLoginQps;
    }
}
