package com.qinghuan.pojo.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BookingOrderStatusTest {

    @Test
    void pendingPaymentShouldSupportPaymentCancelAndTimeout() {
        assertEquals(BookingOrderStatus.PAID,
                BookingOrderStatus.PENDING_PAYMENT.next(BookingOrderEvent.PAY_SUCCESS));
        assertEquals(BookingOrderStatus.CANCELLED,
                BookingOrderStatus.PENDING_PAYMENT.next(BookingOrderEvent.USER_CANCEL));
        assertEquals(BookingOrderStatus.CLOSED,
                BookingOrderStatus.PENDING_PAYMENT.next(BookingOrderEvent.PAYMENT_TIMEOUT));
    }

    @Test
    void paidShouldSupportRefundAndCompletion() {
        assertEquals(BookingOrderStatus.REFUNDING,
                BookingOrderStatus.PAID.next(BookingOrderEvent.REFUND_REQUESTED));
        assertEquals(BookingOrderStatus.COMPLETED,
                BookingOrderStatus.PAID.next(BookingOrderEvent.FULFILLMENT_FINISHED));
        assertEquals(BookingOrderStatus.REFUNDED,
                BookingOrderStatus.REFUNDING.next(BookingOrderEvent.REFUND_SUCCESS));
        assertEquals(BookingOrderStatus.PAID,
                BookingOrderStatus.REFUNDING.next(BookingOrderEvent.REFUND_FAILED));
    }

    @Test
    void unsupportedEventsAndTerminalStatesShouldBeRejected() {
        assertThrows(IllegalStateException.class,
                () -> BookingOrderStatus.PENDING_PAYMENT.next(
                        BookingOrderEvent.REFUND_SUCCESS));
        assertThrows(IllegalStateException.class,
                () -> BookingOrderStatus.COMPLETED.next(BookingOrderEvent.PAY_SUCCESS));
    }
}
