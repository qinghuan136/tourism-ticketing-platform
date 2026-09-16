package com.qinghuan.booking;

import com.qinghuan.common.response.ApiResponse;
import com.qinghuan.pojo.remote.venue.BookingContextDTO;
import com.qinghuan.pojo.remote.venue.BookingContextRequest;
import com.qinghuan.pojo.remote.venue.InventoryChangeRequest;
import com.qinghuan.pojo.remote.venue.InventoryReservationOperationRequest;
import com.qinghuan.pojo.remote.venue.InventoryTryReserveRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** 订单服务访问景点领域的粗粒度远程接口。 */
@FeignClient(name = "venue-service", fallbackFactory = VenueClientFallbackFactory.class)
public interface VenueClient {

    @PostMapping("/internal/venue/booking-context")
    ApiResponse<BookingContextDTO> getBookingContext(
            @RequestBody BookingContextRequest request);

    @PostMapping("/internal/venue/inventory/try-reserve")
    ApiResponse<Void> tryReserve(@RequestBody InventoryTryReserveRequest request);

    @PostMapping("/internal/venue/inventory/confirm-reserve")
    ApiResponse<Void> confirmReserve(@RequestBody InventoryReservationOperationRequest request);

    @PostMapping("/internal/venue/inventory/cancel-reserve")
    ApiResponse<Void> cancelReserve(@RequestBody InventoryReservationOperationRequest request);

    @PostMapping("/internal/venue/inventory/release")
    ApiResponse<Void> releaseInventory(@RequestBody InventoryChangeRequest request);
}
