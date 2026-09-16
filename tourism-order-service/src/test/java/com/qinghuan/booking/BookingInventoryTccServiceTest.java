package com.qinghuan.booking;

import com.qinghuan.pojo.entity.BookingInventoryTccOperation;
import com.qinghuan.pojo.enums.BookingInventoryTccStatus;
import com.qinghuan.pojo.remote.venue.InventoryReservationOperationRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BookingInventoryTccServiceTest {

    @Mock
    private BookingInventoryTccMapper tccMapper;
    @Mock
    private VenueClient venueClient;

    @Test
    void shouldRetryConfirmingOperationIdempotently() {
        BookingInventoryTccOperation operation = new BookingInventoryTccOperation();
        operation.setOrderId(9001L);
        operation.setStatus(BookingInventoryTccStatus.CONFIRMING);

        service().retry(operation);

        verify(venueClient).confirmReserve(new InventoryReservationOperationRequest(9001L));
        verify(tccMapper).updateStatus(9001L, BookingInventoryTccStatus.CONFIRMING,
                BookingInventoryTccStatus.CONFIRMED);
    }

    @Test
    void shouldRetryCancelingOperationIdempotently() {
        BookingInventoryTccOperation operation = new BookingInventoryTccOperation();
        operation.setOrderId(9001L);
        operation.setStatus(BookingInventoryTccStatus.CANCELING);

        service().retry(operation);

        verify(venueClient).cancelReserve(new InventoryReservationOperationRequest(9001L));
        verify(tccMapper).updateStatus(9001L, BookingInventoryTccStatus.CANCELING,
                BookingInventoryTccStatus.CANCELED);
    }

    private BookingInventoryTccService service() {
        return new BookingInventoryTccService(tccMapper, venueClient);
    }
}
