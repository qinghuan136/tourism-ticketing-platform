package com.qinghuan.session;

import com.qinghuan.common.response.ApiResponse;
import com.qinghuan.pojo.remote.venue.BookingContextDTO;
import com.qinghuan.pojo.remote.venue.BookingContextRequest;
import com.qinghuan.pojo.remote.venue.BookingTicketTypeDTO;
import com.qinghuan.pojo.remote.venue.InventoryChangeRequest;
import com.qinghuan.pojo.remote.venue.InventoryReservationOperationRequest;
import com.qinghuan.pojo.remote.venue.InventoryTryReserveRequest;
import com.qinghuan.pojo.remote.venue.VenueSummaryDTO;
import com.qinghuan.pojo.vo.SessionTicketTypeVO;
import com.qinghuan.venue.VenueService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 仅供其他服务调用的景点领域接口。
 * 对外的访问入口由 Gateway 拦截 /internal/**，这里不暴露 Mapper 或实体。
 */
@RestController
@RequestMapping("/internal/venue")
public class InternalVenueController {

    private final SessionService sessionService;
    private final SessionInventoryService sessionInventoryService;
    private final InventoryReservationService inventoryReservationService;
    private final VenueService venueService;

    public InternalVenueController(SessionService sessionService,
                                   SessionInventoryService sessionInventoryService,
                                   InventoryReservationService inventoryReservationService,
                                   VenueService venueService) {
        this.sessionService = sessionService;
        this.sessionInventoryService = sessionInventoryService;
        this.inventoryReservationService = inventoryReservationService;
        this.venueService = venueService;
    }

    /**
     * 下单时同时返回场次归属、开始时间和可售票种价格。
     * 未传票种时只返回场次基础信息，供退款校验场次开始时间复用。
     */
    @PostMapping("/booking-context")
    public ApiResponse<BookingContextDTO> getBookingContext(
            @RequestBody BookingContextRequest request) {
        List<Long> ticketTypeIds = request.sessionTicketTypeIds() == null
                ? List.of()
                : request.sessionTicketTypeIds();
        List<BookingTicketTypeDTO> ticketTypes = ticketTypeIds.isEmpty()
                ? List.of()
                : sessionInventoryService.getOrderableTicketTypes(request.sessionId(), ticketTypeIds)
                .stream()
                .map(this::toBookingTicketType)
                .toList();

        var session = sessionService.getSessionForOrder(request.sessionId());
        Long venueId = session.getVenueId();
        VenueSummaryDTO venue = venueService.getVenueSummary(venueId);
        return ApiResponse.success(new BookingContextDTO(
                request.sessionId(), venueId, venue.venueName(), venue.venueAddress(),
                session.getVisitDate(), session.getStartTime(), session.getEndTime(),
                java.time.LocalDateTime.of(session.getVisitDate(), session.getStartTime()), ticketTypes));
    }

    /** 创建订单时预占场次和票种库存。 */
    @PostMapping("/inventory/reserve")
    public ApiResponse<Void> reserveInventory(@RequestBody InventoryChangeRequest request) {
        sessionInventoryService.reserveInventory(
                request.sessionId(), request.ticketTypeQuantities());
        return ApiResponse.success();
    }

    /** 订单取消、超时或退款后归还场次和票种库存。 */
    @PostMapping("/inventory/release")
    public ApiResponse<Void> releaseInventory(@RequestBody InventoryChangeRequest request) {
        sessionInventoryService.releaseInventory(
                request.sessionId(), request.ticketTypeQuantities());
        return ApiResponse.success();
    }

    /** TCC Try：条件扣减库存并保存可幂等查询的预留记录。 */
    @PostMapping("/inventory/try-reserve")
    public ApiResponse<Void> tryReserve(@RequestBody InventoryTryReserveRequest request) {
        inventoryReservationService.tryReserve(
                request.orderId(), request.sessionId(), request.ticketTypeQuantities());
        return ApiResponse.success();
    }

    /** TCC Confirm：只确认预留归属，不重复扣减库存。 */
    @PostMapping("/inventory/confirm-reserve")
    public ApiResponse<Void> confirmReserve(@RequestBody InventoryReservationOperationRequest request) {
        inventoryReservationService.confirmReserve(request.orderId());
        return ApiResponse.success();
    }

    /** TCC Cancel：仅撤销仍处于 RESERVED 的预留并恢复一次库存。 */
    @PostMapping("/inventory/cancel-reserve")
    public ApiResponse<Void> cancelReserve(@RequestBody InventoryReservationOperationRequest request) {
        inventoryReservationService.cancelReserve(request.orderId());
        return ApiResponse.success();
    }

    /** 为优惠券等服务提供最小景点快照，不暴露景点实体。 */
    @GetMapping("/summary/{venueId}")
    public ApiResponse<VenueSummaryDTO> getVenueSummary(@PathVariable Long venueId) {
        return ApiResponse.success(venueService.getVenueSummary(venueId));
    }

    private BookingTicketTypeDTO toBookingTicketType(SessionTicketTypeVO source) {
        return new BookingTicketTypeDTO(
                source.getSessionTicketTypeId(),
                source.getTicketTypeName(),
                source.getSalePrice());
    }
}
