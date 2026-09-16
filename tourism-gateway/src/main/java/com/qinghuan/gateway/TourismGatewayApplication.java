package com.qinghuan.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.qinghuan")
public class TourismGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(TourismGatewayApplication.class, args);
    }
}
