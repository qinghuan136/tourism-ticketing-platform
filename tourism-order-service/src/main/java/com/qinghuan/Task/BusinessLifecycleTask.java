package com.qinghuan.Task;

import com.qinghuan.booking.BookingService;
import com.qinghuan.ticket.TicketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** 定时收口订单与电子票的最终状态。场次状态由 venue-service 维护。 */
@Slf4j
@Component
public class BusinessLifecycleTask {

    private final TicketService ticketService;
    private final BookingService bookingService;

    public BusinessLifecycleTask(TicketService ticketService,
                                 BookingService bookingService) {
        this.ticketService = ticketService;
        this.bookingService = bookingService;
    }

    @Scheduled(cron = "30 * * * * *")
    public void maintainLifecycle() {
        LocalDateTime now = LocalDateTime.now();

        int tickets = ticketService.expireTickets(now);
        int orders = bookingService.completePaidOrders(now);

        if (tickets + orders > 0) {
            log.info("订单状态收口完成：票券 {}，订单 {}", tickets, orders);
        }
    }
}
