package com.qinghuan.Task;

import com.qinghuan.booking.BookingService;
import com.qinghuan.session.SessionService;
import com.qinghuan.ticket.TicketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** 定时收口场次、电子票和订单的最终状态。 */
@Slf4j
@Component
public class BusinessLifecycleTask {

    private final SessionService sessionService;
    private final TicketService ticketService;
    private final BookingService bookingService;

    public BusinessLifecycleTask(SessionService sessionService,
                                 TicketService ticketService,
                                 BookingService bookingService) {
        this.sessionService = sessionService;
        this.ticketService = ticketService;
        this.bookingService = bookingService;
    }

    @Scheduled(cron = "30 * * * * *")
    public void maintainLifecycle() {
        LocalDateTime now = LocalDateTime.now();

        // 先结束场次，再收口依赖场次状态的票券和订单。
        int sessions = sessionService.maintainLifecycle(now);
        int tickets = ticketService.expireTickets(now);
        int orders = bookingService.completePaidOrders(now);

        if (sessions + tickets + orders > 0) {
            log.info("业务状态收口完成：场次 {}，票券 {}，订单 {}", sessions, tickets, orders);
        }
    }
}
