package com.qinghuan.session;

import com.qinghuan.common.response.ApiResponse;
import com.qinghuan.pojo.remote.order.SessionOrderStateDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** 场次生命周期只通过订单服务读取订单占用状态。 */
@FeignClient(name = "order-service", configuration = ServiceTokenFeignConfig.class)
public interface OrderClient {

    @GetMapping("/internal/orders/sessions/{sessionId}/state")
    ApiResponse<SessionOrderStateDTO> getSessionOrderState(@PathVariable("sessionId") Long sessionId);
}
