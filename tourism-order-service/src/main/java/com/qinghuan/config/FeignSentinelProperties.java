package com.qinghuan.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 订单服务依赖的远程服务熔断阈值。 */
@ConfigurationProperties(prefix = "app.sentinel.feign")
public class FeignSentinelProperties {

    private double venueExceptionRatio = 0.5;
    private double couponExceptionRatio = 0.6;

    public double getVenueExceptionRatio() {
        return venueExceptionRatio;
    }

    public void setVenueExceptionRatio(double venueExceptionRatio) {
        this.venueExceptionRatio = venueExceptionRatio;
    }

    public double getCouponExceptionRatio() {
        return couponExceptionRatio;
    }

    public void setCouponExceptionRatio(double couponExceptionRatio) {
        this.couponExceptionRatio = couponExceptionRatio;
    }
}
