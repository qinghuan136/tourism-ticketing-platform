package com.qinghuan.booking;

import com.qinghuan.common.exception.BusinessException;
import com.qinghuan.common.response.ApiResponse;
import com.qinghuan.pojo.remote.venue.BookingContextDTO;
import com.qinghuan.pojo.remote.venue.BookingContextRequest;
import com.qinghuan.pojo.remote.venue.InventoryChangeRequest;
import com.qinghuan.pojo.remote.venue.InventoryReservationOperationRequest;
import com.qinghuan.pojo.remote.venue.InventoryTryReserveRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/** 熔断或调用失败时，库存相关操作必须失败，不能伪造成功。 */
@Component
public class VenueClientFallbackFactory implements FallbackFactory<VenueClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(VenueClientFallbackFactory.class);

    @Override
    public VenueClient create(Throwable cause) {
        LOGGER.warn("venue-service 调用失败或已熔断: {}", cause.toString());
        return new VenueClient() {
            @Override
            public ApiResponse<BookingContextDTO> getBookingContext(BookingContextRequest request) {
                throw unavailable();
            }

            @Override
            public ApiResponse<Void> tryReserve(InventoryTryReserveRequest request) {
                throw unavailable();
            }

            @Override
            public ApiResponse<Void> confirmReserve(InventoryReservationOperationRequest request) {
                throw unavailable();
            }

            @Override
            public ApiResponse<Void> cancelReserve(InventoryReservationOperationRequest request) {
                throw unavailable();
            }

            @Override
            public ApiResponse<Void> releaseInventory(InventoryChangeRequest request) {
                throw unavailable();
            }

            private RemoteServiceUnavailableException unavailable() {
                if (cause instanceof BusinessException businessException) {
                    // 下游已返回明确业务错误时保留其语义，不能伪装成服务不可用。
                    throw businessException;
                }
                return new RemoteServiceUnavailableException("场次库存服务暂时不可用", cause);
            }
        };
    }
}
