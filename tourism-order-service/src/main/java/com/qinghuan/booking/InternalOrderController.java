package com.qinghuan.booking;

import com.qinghuan.common.response.ApiResponse;
import com.qinghuan.pojo.remote.order.SessionOrderStateDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 场次服务使用的订单占用查询，不暴露订单实体或 Mapper。 */
@RestController
@RequestMapping("/internal/orders")
public class InternalOrderController {

    private final BookingService bookingService;

    public InternalOrderController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/sessions/{sessionId}/state")
    public ApiResponse<SessionOrderStateDTO> getSessionOrderState(@PathVariable Long sessionId) {
        return ApiResponse.success(new SessionOrderStateDTO(
                bookingService.hasOrdersForSession(sessionId),
                bookingService.hasUnresolvedOrdersForSession(sessionId)));
    }
}
