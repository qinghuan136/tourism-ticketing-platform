package com.qinghuan.booking;

import com.qinghuan.common.response.ApiResponse;
import com.qinghuan.pojo.remote.user.VisitorForOrderDTO;
import com.qinghuan.pojo.remote.user.CurrentUserProfileDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/** 订单域通过用户服务校验当前游客名下的参观人。 */
@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/internal/users/active-visitors")
    ApiResponse<List<VisitorForOrderDTO>> listActiveVisitorsForOrder();

    @GetMapping("/internal/users/current-profile")
    ApiResponse<CurrentUserProfileDTO> getCurrentProfile();
}
