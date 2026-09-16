package com.qinghuan.booking;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import cn.hutool.core.util.IdUtil;
import com.qinghuan.annotation.RefreshCreateTimeOrUpdateTime;
import com.qinghuan.auth.context.UserContext;
import com.qinghuan.booking.refund.RefundGateway;
import com.qinghuan.booking.refund.RefundGatewayResult;
import com.qinghuan.booking.refund.RefundRequest;
import com.qinghuan.common.exception.BusinessException;
import com.qinghuan.common.exception.ErrorCode;
import com.qinghuan.pojo.dto.OrderCreateDTO;
import com.qinghuan.pojo.dto.OrderCreateItemRequest;
import com.qinghuan.pojo.dto.OrderPageQueryDTO;
import com.qinghuan.pojo.dto.VenueOrderPageQueryDTO;
import com.qinghuan.pojo.entity.BookingOrder;
import com.qinghuan.pojo.entity.BookingOrderItem;
import com.qinghuan.pojo.entity.Ticket;
import com.qinghuan.pojo.enums.BookingOrderEvent;
import com.qinghuan.pojo.enums.BookingOrderStatus;
import com.qinghuan.pojo.enums.TicketStatus;
import com.qinghuan.pojo.remote.user.VisitorForOrderDTO;
import com.qinghuan.pojo.remote.user.CurrentUserProfileDTO;
import com.qinghuan.pojo.remote.coupon.CouponOrderTryRequest;
import com.qinghuan.pojo.remote.coupon.CouponOrderTryResponse;
import com.qinghuan.pojo.remote.venue.BookingContextDTO;
import com.qinghuan.pojo.remote.venue.BookingContextRequest;
import com.qinghuan.pojo.remote.venue.BookingTicketTypeDTO;
import com.qinghuan.pojo.remote.venue.InventoryChangeRequest;
import com.qinghuan.pojo.vo.*;
import com.qinghuan.ticket.TicketService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.qinghuan.common.constant.cacheKeys.LockConstant.LOCK_BOOKING_PREFIX;

/**
 * 订单业务实现。
 * 创建订单时由后端校验参观人和场次票种，并保存不可变的下单快照。
 */
@Service
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingMapper bookingMapper;
    private final BookingInventoryTccService inventoryTccService;
    private final UserClient userClient;
    private final VenueClient venueClient;
    private final TicketService ticketService;
    private final BookingCouponTccService couponTccService;
    private final RefundGateway refundGateway;
    private final RedissonClient redisson;
    private final TransactionTemplate transactionTemplate;

    public BookingServiceImpl(BookingMapper bookingMapper,
                              BookingInventoryTccService inventoryTccService,
                              UserClient userClient,
                              VenueClient venueClient,
                              TicketService ticketService,
                              BookingCouponTccService couponTccService,
                              RefundGateway refundGateway,
                              RedissonClient redisson, TransactionTemplate transactionTemplate) {
        this.bookingMapper = bookingMapper;
        this.inventoryTccService = inventoryTccService;
        this.userClient = userClient;
        this.venueClient = venueClient;
        this.ticketService = ticketService;
        this.couponTccService = couponTccService;
        this.refundGateway = refundGateway;
        this.redisson = redisson;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * 分页查询当前游客的订单。
     * userId 只从登录上下文获取，避免客户端通过查询参数越权读取他人订单。
     */
    @Override
    public PageResult<OrderSummaryVO> pageMyOrders(OrderPageQueryDTO queryDTO) {
        PageHelper.startPage(queryDTO.getPage(), queryDTO.getSize());
        Page<OrderSummaryVO> page = (Page<OrderSummaryVO>) bookingMapper.listMyOrders(
                UserContext.getRequired().userId(), queryDTO.getStatus());
        return toPageResult(page);
    }

    /** 获取当前游客的订单头，并一次性补充全部明细及票券。 */
    @Override
    public OrderDetailVO getMyOrder(Long orderId) {
        OrderDetailVO detail = bookingMapper.findMyOrderDetail(
                orderId, UserContext.getRequired().userId());
        return completeOrderDetail(detail, orderId);
    }

    /**
     * 运营端分页查询只使用 JWT 中的 venueId 作为数据范围。
     * 订单号、场次和参观日期都只是当前景点范围内的附加筛选条件。
     */
    @Override
    public PageResult<OrderSummaryVO> pageVenueOrders(VenueOrderPageQueryDTO queryDTO) {
        PageHelper.startPage(queryDTO.getPage(), queryDTO.getSize());
        Page<OrderSummaryVO> page = (Page<OrderSummaryVO>) bookingMapper.listVenueOrders(
                UserContext.getRequired().venueId(), queryDTO);
        return toPageResult(page);
    }

    /** 获取当前景点的订单详情，并补充购买人、明细及票券信息。 */
    @Override
    public OrderDetailVO getVenueOrder(Long orderId) {
        OrderDetailVO detail = bookingMapper.findVenueOrderDetail(
                orderId, UserContext.getRequired().venueId());
        return completeOrderDetail(detail, orderId);
    }

    /**
     * 模拟支付待支付订单，并在同一事务中生成电子票。
     * 订单状态条件更新成功后，本事务才拥有后续生成票券的执行权。
     */
    @Override
    @Transactional
    public boolean payOrder(Long orderId) {
        BookingOrder order = bookingMapper.findOrderByOrderId(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        if (!order.getUserId().equals(UserContext.getRequired().userId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该订单");
        }

        BookingOrderStatus oldStatus = order.getStatus();
        BookingOrderStatus paidStatus;
        try {
            paidStatus = oldStatus.next(BookingOrderEvent.PAY_SUCCESS);
        } catch (IllegalStateException exception) {
            throw new BusinessException(ErrorCode.CONFLICT, "只有待支付订单可以支付");
        }

        LocalDateTime now = LocalDateTime.now();
        if (!now.isBefore(order.getExpireAt())) {
            closeExpiredOrder(order, oldStatus, now);
            // 不在事务中抛异常，确保关闭订单和归还库存能够正常提交。
            return false;
        }

        order.setPaymentNo(UUID.randomUUID().toString().replace("-", ""));
        order.setPaidAt(now);
        order.setStatus(paidStatus);
        int updated = bookingMapper.updatePaidOrder(order, oldStatus);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "订单状态或支付期限发生变化");
        }

        // 本地支付事务提交后再 Confirm；状态先落为 CONFIRMING，失败由任务重试。
        confirmCouponAfterCommit(order, now);

        // 票券与支付状态在同一事务中落库，任一失败都会整体回滚。
        ticketService.createTicketsForOrder(orderId, order.getQuantity());
        return true;
    }

    /** 关闭支付时已经超时的订单，并归还创建订单时预占的库存。 */
    private void closeExpiredOrder(BookingOrder order,
                                   BookingOrderStatus oldStatus,
                                   LocalDateTime closedAt) {
        order.setClosedAt(closedAt);
        order.setStatus(oldStatus.next(BookingOrderEvent.PAYMENT_TIMEOUT));
        if (bookingMapper.updateOrder(order, oldStatus) == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "订单状态发生变化");
        }

        Map<Long, Integer> ticketTypeQuantities = bookingMapper.listOrderItems(order.getId())
                .stream()
                .collect(Collectors.groupingBy(
                        OrderItemVO::getSessionTicketTypeId,
                        Collectors.summingInt(item -> 1)));
        releaseInventory(order.getSessionId(), ticketTypeQuantities);
        cancelCouponAfterCommit(order, closedAt);
    }

    /**
     * 整单退款分成两个本地事务，第三方请求在两个事务之间执行。
     * 这样既不长时间占用数据库锁，也能保留已发起退款的意图。
     */
    @Override
    public void refundOrder(Long orderId) {
        Long userId = UserContext.getRequired().userId();
        RefundPreparation preparation = transactionTemplate.execute(
                status -> prepareRefund(orderId, userId));
        if (preparation.alreadyCompleted()) {
            return;
        }
        processRefund(preparation.request(), preparation.newRequest());
    }

    /** 第一段事务：保存退款意图、固定 refundNo，并冻结票券。 */
    private RefundPreparation prepareRefund(Long orderId, Long userId) {
        BookingOrder order = bookingMapper.findOrderByOrderId(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该订单");
        }
        if (order.getStatus() == BookingOrderStatus.REFUNDED) {
            return RefundPreparation.done();
        }
        if (order.getStatus() == BookingOrderStatus.REFUNDING) {
            return RefundPreparation.pending(toRefundRequest(order), false);
        }

        BookingOrderStatus oldStatus = order.getStatus();
        try {
            order.setStatus(oldStatus.next(BookingOrderEvent.REFUND_REQUESTED));
        } catch (IllegalStateException exception) {
            throw new BusinessException(ErrorCode.CONFLICT, "只有已支付订单可以退款");
        }

        LocalDateTime now = LocalDateTime.now();
        BookingContextDTO bookingContext = getBookingContext(order.getSessionId(), List.of());
        if (!now.isBefore(bookingContext.sessionStartTime())) {
            throw new BusinessException(ErrorCode.CONFLICT, "场次已开始，不能进行整单退款");
        }

        List<Ticket> tickets = ticketService.listTicketsByOrderId(orderId);
        if (tickets.stream().anyMatch(ticket -> ticket.getStatus() != TicketStatus.VALID)) {
            throw new BusinessException(ErrorCode.CONFLICT, "订单票券当前不可退款");
        }

        order.setRefundNo("RF" + UUID.randomUUID().toString().replace("-", ""));
        order.setRefundRequestedAt(now);
        if (bookingMapper.updateOrder(order, oldStatus) == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "订单状态发生变化");
        }

        // 冻结票券后，并发核销的 VALID -> USED 更新会失败。
        if (ticketService.markRefunding(orderId) != tickets.size()) {
            throw new BusinessException(ErrorCode.CONFLICT, "票券状态发生变化，退款失败");
        }
        return RefundPreparation.pending(toRefundRequest(order), true);
    }

    /** 事务外请求或查询退款平台，超时时保持 REFUNDING 等待定时对账。 */
    private void processRefund(RefundRequest request, boolean newRequest) {
        RefundGatewayResult result;
        try {
            result = newRequest
                    ? refundGateway.requestRefund(request)
                    : refundGateway.queryRefund(request.refundNo());
            // 第一段事务提交后、调用平台前宕机，重启后使用原 refundNo 补发。
            if (!newRequest && result == RefundGatewayResult.NOT_FOUND) {
                result = refundGateway.requestRefund(request);
            }
        } catch (RuntimeException exception) {
            log.warn("退款平台结果暂时无法确认，订单保持 REFUNDING，refundNo={}",
                    request.refundNo(), exception);
            return;
        }

        if (result == RefundGatewayResult.SUCCESS) {
            transactionTemplate.execute(status -> {
                completeRefund(request.orderId());
                return null;
            });
        } else if (result == RefundGatewayResult.FAILED) {
            transactionTemplate.execute(status -> {
                failRefund(request.orderId());
                return null;
            });
        }
    }

    /** 第二段事务：确认到账后统一完成订单、票券、库存和优惠券收尾。 */
    private void completeRefund(Long orderId) {
        BookingOrder order = bookingMapper.findOrderByOrderId(orderId);
        if (order.getStatus() == BookingOrderStatus.REFUNDED) {
            return;
        }
        if (order.getStatus() != BookingOrderStatus.REFUNDING) {
            throw new BusinessException(ErrorCode.CONFLICT, "订单不在退款中");
        }

        List<Ticket> tickets = ticketService.listTicketsByOrderId(orderId);
        if (tickets.stream().anyMatch(ticket -> ticket.getStatus() != TicketStatus.REFUNDING)) {
            throw new BusinessException(ErrorCode.CONFLICT, "退款票券状态发生变化");
        }
        Map<Long, Integer> ticketTypeQuantities = bookingMapper.listOrderItems(orderId).stream()
                .collect(Collectors.groupingBy(
                        OrderItemVO::getSessionTicketTypeId,
                        Collectors.summingInt(item -> 1)));

        BookingOrderStatus oldStatus = order.getStatus();
        order.setStatus(oldStatus.next(BookingOrderEvent.REFUND_SUCCESS));
        order.setRefundAt(LocalDateTime.now());
        if (bookingMapper.updateOrder(order, oldStatus) == 0) {
            return;
        }
        if (ticketService.completeRefund(orderId) != tickets.size()) {
            throw new BusinessException(ErrorCode.CONFLICT, "退款票券状态发生变化");
        }

        releaseInventory(order.getSessionId(), ticketTypeQuantities);
        restoreCouponAfterCommit(order, order.getRefundAt());
    }

    /** 第三方明确拒绝退款时恢复订单和票券，不归还库存。 */
    private void failRefund(Long orderId) {
        BookingOrder order = bookingMapper.findOrderByOrderId(orderId);
        if (order.getStatus() != BookingOrderStatus.REFUNDING) {
            return;
        }
        List<Ticket> tickets = ticketService.listTicketsByOrderId(orderId);
        BookingOrderStatus paidStatus = order.getStatus().next(BookingOrderEvent.REFUND_FAILED);
        if (bookingMapper.resetFailedRefund(orderId, paidStatus) == 0) {
            return;
        }
        if (ticketService.cancelRefund(orderId, LocalDateTime.now()) != tickets.size()) {
            throw new BusinessException(ErrorCode.CONFLICT, "退款票券解冻失败");
        }
    }

    @Override
    public void reconcileRefund(Long orderId) {
        BookingOrder order = bookingMapper.findOrderByOrderId(orderId);
        if (order == null || order.getStatus() != BookingOrderStatus.REFUNDING) {
            return;
        }
        processRefund(toRefundRequest(order), false);
    }

    @Override
    public List<BookingOrder> listRefundingOrders(LocalDateTime requestedBefore) {
        return bookingMapper.listRefundingOrders(requestedBefore);
    }

    private RefundRequest toRefundRequest(BookingOrder order) {
        return new RefundRequest(
                order.getId(), order.getRefundNo(), order.getPaymentNo(), order.getTotalAmount());
    }

    private record RefundPreparation(
            RefundRequest request, boolean newRequest, boolean alreadyCompleted) {

        private static RefundPreparation pending(RefundRequest request, boolean newRequest) {
            return new RefundPreparation(request, newRequest, false);
        }

        private static RefundPreparation done() {
            return new RefundPreparation(null, false, true);
        }
    }

    @Override
    @Transactional
    public void cancelOrder(Long orderId) {
        // 获取订单信息
        BookingOrder order = bookingMapper.findOrderByOrderId(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        // 判断是否有权限
        if (!order.getUserId().equals(UserContext.getRequired().userId())) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN, "无权访问该订单");
        }
        // 状态转换
        BookingOrderStatus oldStatus = order.getStatus();
        BookingOrderStatus newStatus;
        try {
            newStatus = oldStatus.next(BookingOrderEvent.USER_CANCEL);
        } catch (IllegalStateException exception) {
            // 用户取消只适用于仍处于待支付状态的订单。
            throw new BusinessException(ErrorCode.CONFLICT, "只有待支付订单可以取消");
        }
        // 更新订单信息
        order.setCancelledAt(LocalDateTime.now());
        order.setStatus(newStatus);
        Integer okNumber = bookingMapper.updateOrder(order, oldStatus);
        if (okNumber == 0) {
            throw new BusinessException(
                    ErrorCode.CONFLICT, "订单状态发生变化");
        }

        // 获取订单详情
        List<OrderItemVO> items = bookingMapper.listOrderItems(orderId);

        // 归还场次容量和sessionTicketType容量
        releaseInventory(order.getSessionId(), items.stream().collect(
                Collectors.groupingBy(OrderItemVO::getSessionTicketTypeId, Collectors.summingInt(item -> 1))));
        cancelCouponAfterCommit(order, order.getCancelledAt());
    }

    @Override
    @Transactional
    public void cancelTimeoutOrder(Long id) {
        BookingOrder order = bookingMapper.findOrderByOrderId(id);
        // PAYMENT_TIMEOUT 对应 CLOSED，记录超时关闭时间而不是用户取消时间。
        order.setClosedAt(LocalDateTime.now());
        BookingOrderStatus oldStatus = order.getStatus();
        order.setStatus(oldStatus.next(BookingOrderEvent.PAYMENT_TIMEOUT));
        Integer okNumber = bookingMapper.updateOrder(order, oldStatus);
        if (okNumber == 0) {
            throw new BusinessException(
                    ErrorCode.CONFLICT, "超时订单取消失败");
        }

        // 获取订单详情
        List<OrderItemVO> items = bookingMapper.listOrderItems(order.getId());
        // 释放库存
        releaseInventory(order.getSessionId(), items.stream().collect(
                Collectors.groupingBy(OrderItemVO::getSessionTicketTypeId, Collectors.summingInt(item -> 1))));
        cancelCouponAfterCommit(order, order.getClosedAt());

    }

    @Override
    public List<BookingOrder> listTimeoutOrders() {
        return bookingMapper.listTimeoutOrders(LocalDateTime.now());
    }

    @Override
    public int completePaidOrders(LocalDateTime now) {
        return bookingMapper.completePaidOrders(now);
    }

    @Override
    public boolean hasOrdersForSession(Long sessionId) {
        return bookingMapper.hasOrdersForSession(sessionId);
    }

    @Override
    public boolean hasUnresolvedOrdersForSession(Long sessionId) {
        return bookingMapper.hasUnresolvedOrdersForSession(sessionId);
    }


    /** 创建订单、明细快照并预占库存，任一步失败都回滚。 */
    @Override
    public OrderCreatedVO createOrder(OrderCreateDTO createOrderDTO) {
        // 数据库同样限制一个订单内参观人唯一；这里提前返回更明确的业务错误。
        List<Long> visitorIds = createOrderDTO.items().stream()
                .map(OrderCreateItemRequest::visitorId)
                .sorted()
                .toList();
        if (new HashSet<>(visitorIds).size() != visitorIds.size()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "同一个参观人不能重复下单");
        }

        // 只允许使用当前游客名下且已启用的参观人，同时保留原始证件信息用于订单快照。
        Map<Long, VisitorForOrderDTO> visitorsById = userClient.listActiveVisitorsForOrder().data().stream()
                .collect(Collectors.toMap(VisitorForOrderDTO::id, Function.identity()));
        if (!visitorsById.keySet().containsAll(visitorIds)) {
            throw new BusinessException(
                    ErrorCode.CONFLICT, "参观人不存在、不属于当前游客或已停用");
        }

        List<String> fingerprints = createOrderDTO.items().stream()
                .map(item -> visitorsById.get(item.visitorId()).fingerprint())
                .toList();
        if (new HashSet<>(fingerprints).size() != fingerprints.size()) {
            throw new BusinessException(ErrorCode.CONFLICT, "同一证件身份不能重复下单");
        }
        List<String> sortedFingerprints = fingerprints.stream().sorted().toList();
        // 购买人展示信息同样作为订单头快照保存，运营端详情不再查询 user_account。
        CurrentUserProfileDTO purchaser = userClient.getCurrentProfile().data();

        List<RLock> acquiredLocks = new ArrayList<>();
        try {
            // 按稳定身份指纹排序加锁，避免多人订单的跨实例锁顺序不一致。
            for (String fingerprint : sortedFingerprints) {
                String lockKey = LOCK_BOOKING_PREFIX
                        + createOrderDTO.sessionId() + ":" + fingerprint;
                RLock lock = redisson.getLock(lockKey);
                if (!lock.tryLock()) {
                    throw new BusinessException(
                            ErrorCode.CONFLICT, "参观身份正在当前场次下单");
                }
                acquiredLocks.add(lock);
            }

            // 先由场次服务校验并预占库存。订单明细带有场次票种外键，若先写明细，
            // 本地事务会持有该行的共享锁，远程库存更新将无法取得排他锁。
            Map<Long, Integer> ticketTypeQuantities = createOrderDTO.items().stream()
                    .collect(Collectors.groupingBy(
                            OrderCreateItemRequest::sessionTicketTypeId,
                            Collectors.summingInt(item -> 1)));
            BookingContextDTO bookingContext = getBookingContext(
                    createOrderDTO.sessionId(), ticketTypeQuantities.keySet().stream().toList());

            // TCC 使用应用生成的订单ID，使库存预留在订单头尚未写入前也有稳定业务键。
            Long orderId = IdUtil.getSnowflakeNextId();
            inventoryTccService.start(orderId, createOrderDTO.sessionId());
            if (createOrderDTO.userCouponId() != null) {
                couponTccService.start(orderId, createOrderDTO.userCouponId());
            }
            try {
                inventoryTccService.tryReserve(orderId, createOrderDTO.sessionId(), ticketTypeQuantities);

                CouponOrderTryResponse couponSnapshot = createOrderDTO.userCouponId() == null
                        ? null
                        : couponTccService.tryLock(new CouponOrderTryRequest(
                            orderId, createOrderDTO.userCouponId(), UserContext.getRequired().userId(),
                            bookingContext.venueId(), calculateOriginalAmount(createOrderDTO, bookingContext)));

                // TransactionTemplate 返回前事务已经提交，因此这些锁会一直持有到订单真正落库。
                OrderCreatedVO result = transactionTemplate.execute(status ->
                        createInTransaction(createOrderDTO, visitorsById, sortedFingerprints,
                                bookingContext, purchaser, orderId, couponSnapshot));
                try {
                    inventoryTccService.confirm(orderId);
                } catch (RuntimeException confirmException) {
                    // 订单已提交，协调记录保持 CONFIRMING，定时任务会重试幂等 Confirm。
                    log.error("订单已创建但库存 Confirm 未完成, orderId={}", orderId, confirmException);
                }
                if (couponSnapshot != null && result.status() == BookingOrderStatus.PAID) {
                    confirmCoupon(orderId, couponSnapshot.couponId(), LocalDateTime.now());
                }
                return result;
            } catch (RuntimeException exception) {
                // TransactionTemplate 抛出异常时本地事务已经回滚，再执行远程 Cancel 不会形成锁等待。
                try {
                    inventoryTccService.markCanceling(orderId);
                    inventoryTccService.cancel(orderId);
                } catch (RuntimeException cancelException) {
                    // Cancel 的失败状态会持久化为 CANCELING，由定时任务补偿，不能静默忽略。
                    log.error("订单创建失败后库存 Cancel 未完成, orderId={}", orderId, cancelException);
                }
                if (createOrderDTO.userCouponId() != null) {
                    try {
                        couponTccService.markCanceling(orderId);
                        couponTccService.cancel(orderId, createOrderDTO.userCouponId(), LocalDateTime.now());
                    } catch (RuntimeException cancelException) {
                        log.error("订单创建失败后优惠券 Cancel 未完成, orderId={}", orderId, cancelException);
                    }
                }
                throw exception;
            }
        } finally {
            // 仅释放当前线程已持有的锁，逆序释放与获取顺序对应。
            for (int i = acquiredLocks.size() - 1; i >= 0; i--) {
                RLock lock = acquiredLocks.get(i);
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
    }

    private OrderCreatedVO createInTransaction(OrderCreateDTO createOrderDTO,
                                                Map<Long, VisitorForOrderDTO> visitorsById,
                                                List<String> fingerprints,
                                                BookingContextDTO bookingContext,
                                                CurrentUserProfileDTO purchaser,
                                                Long orderId,
                                                CouponOrderTryResponse couponSnapshot) {
        // 锁内再次查询有效订单，与后续写订单处于同一数据库事务。
        List<BookingOrder> orders = bookingMapper.findConflictingOrdersBySessionAndFingerprints(
                createOrderDTO.sessionId(), fingerprints);
        if (!orders.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.CONFLICT, "参观身份已在当前场次下单");
        }

        // 场次模块已在预占库存前统一校验开放时间、票种归属、销售状态和可售余量。
        Map<Long, BookingTicketTypeDTO> ticketTypesById = bookingContext.ticketTypes().stream()
                .collect(Collectors.toMap(
                        BookingTicketTypeDTO::sessionTicketTypeId,
                        Function.identity()));

        // 价格只能取后端保存的场次售价，不能信任前端传入的金额。
        BigDecimal originalAmount = calculateOriginalAmount(createOrderDTO, bookingContext);

        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal totalAmount = originalAmount;
        if (couponSnapshot != null) {
            discountAmount = couponSnapshot.discountAmount();
            totalAmount = couponSnapshot.payableAmount();
        }

        // 订单号使用无分隔符 UUID，长度正好符合 order_no 的 32 字符限制。
        BookingOrder bookingOrder = new BookingOrder();
        bookingOrder.setId(orderId);
        bookingOrder.setOrderNo(UUID.randomUUID().toString().replace("-", ""));
        bookingOrder.setUserId(UserContext.getRequired().userId());
        bookingOrder.setSessionId(createOrderDTO.sessionId());
        bookingOrder.setVenueId(bookingContext.venueId());
        bookingOrder.setVenueNameSnapshot(bookingContext.venueName());
        bookingOrder.setVenueAddressSnapshot(bookingContext.venueAddress());
        bookingOrder.setVisitDate(bookingContext.visitDate());
        bookingOrder.setStartTime(bookingContext.startTime());
        bookingOrder.setEndTime(bookingContext.endTime());
        bookingOrder.setPurchaserNameSnapshot(purchaser.displayName());
        bookingOrder.setPurchaserPhoneSnapshot(purchaser.phone());
        bookingOrder.setQuantity(createOrderDTO.items().size());
        bookingOrder.setUserCouponId(createOrderDTO.userCouponId());
        bookingOrder.setOriginalAmount(originalAmount);
        bookingOrder.setDiscountAmount(discountAmount);
        bookingOrder.setTotalAmount(totalAmount);

        // 零元订单无需经过支付接口，创建后直接视为已支付。
        if (totalAmount.signum() == 0) {
            bookingOrder.setStatus(BookingOrderStatus.PAID);
            bookingOrder.setPaidAt(LocalDateTime.now());
        } else {
            bookingOrder.setStatus(BookingOrderStatus.PENDING_PAYMENT);
            bookingOrder.setExpireAt(LocalDateTime.now().plusMinutes(15));
        }

        // 主键回填后才能为每条订单明细设置 orderId。
        bookingMapper.insertOrder(bookingOrder);

        List<BookingOrderItem> orderItems = createOrderDTO.items().stream()
                .map(item -> toOrderItem(
                        bookingOrder.getId(), item,
                        visitorsById.get(item.visitorId()),
                        ticketTypesById.get(item.sessionTicketTypeId())))
                .toList();
        bookingMapper.insertOrderItems(orderItems);

        // 免费票在创建订单的同一事务中生成，避免出现已支付但无票的订单。
        if (bookingOrder.getStatus() == BookingOrderStatus.PAID) {
            ticketService.createTicketsForOrder(
                    bookingOrder.getId(), bookingOrder.getQuantity());
        }

        // 与订单、明细和票券处于同一事务；提交后才允许发起 TCC Confirm。
        inventoryTccService.markConfirming(orderId);
        if (bookingOrder.getStatus() == BookingOrderStatus.PAID && bookingOrder.getUserCouponId() != null) {
            couponTccService.markConfirming(orderId);
        }

        return new OrderCreatedVO(
                bookingOrder.getId(), bookingOrder.getOrderNo(),
                bookingOrder.getTotalAmount(), bookingOrder.getStatus(),
                bookingOrder.getExpireAt());
    }

    /** 将当前资料和成交价格复制为订单明细快照，防止后续资料修改影响历史订单。 */
    private BookingOrderItem toOrderItem(
            Long orderId, OrderCreateItemRequest request,
            VisitorForOrderDTO visitor, BookingTicketTypeDTO ticketType) {
        BookingOrderItem orderItem = new BookingOrderItem();
        orderItem.setOrderId(orderId);
        orderItem.setVisitorId(visitor.id());
        orderItem.setSessionTicketTypeId(request.sessionTicketTypeId());
        orderItem.setVisitorName(visitor.name());
        orderItem.setVisitorIdType(visitor.idType());
        orderItem.setVisitorIdNumber(visitor.idNumber());
        orderItem.setVisitorFingerprint(visitor.fingerprint());
        orderItem.setTicketTypeName(ticketType.ticketTypeName());
        orderItem.setUnitPrice(ticketType.salePrice());
        return orderItem;
    }

    /** 读取一次场次上下文，避免订单服务分别远程查询场次、票种和价格。 */
    private BookingContextDTO getBookingContext(Long sessionId, List<Long> sessionTicketTypeIds) {
        return venueClient.getBookingContext(
                new BookingContextRequest(sessionId, sessionTicketTypeIds)).data();
    }

    private void releaseInventory(Long sessionId, Map<Long, Integer> ticketTypeQuantities) {
        venueClient.releaseInventory(new InventoryChangeRequest(sessionId, ticketTypeQuantities));
    }

    private BigDecimal calculateOriginalAmount(OrderCreateDTO createOrderDTO, BookingContextDTO bookingContext) {
        Map<Long, BookingTicketTypeDTO> ticketTypesById = bookingContext.ticketTypes().stream()
                .collect(Collectors.toMap(BookingTicketTypeDTO::sessionTicketTypeId, Function.identity()));
        return createOrderDTO.items().stream()
                .map(item -> ticketTypesById.get(item.sessionTicketTypeId()).salePrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** 订单提交成功后再远程 Confirm，失败时保留 CONFIRMING 供任务重放。 */
    private void confirmCoupon(Long orderId, Long couponId, LocalDateTime now) {
        try {
            couponTccService.confirm(orderId, couponId, now);
        } catch (RuntimeException exception) {
            log.error("订单已提交但优惠券 Confirm 未完成, orderId={}", orderId, exception);
        }
    }

    private void confirmCouponAfterCommit(BookingOrder order, LocalDateTime now) {
        if (order.getUserCouponId() == null) {
            return;
        }
        couponTccService.markConfirming(order.getId());
        afterCommit(() -> confirmCoupon(order.getId(), order.getUserCouponId(), now));
    }

    private void cancelCouponAfterCommit(BookingOrder order, LocalDateTime now) {
        if (order.getUserCouponId() == null) {
            return;
        }
        couponTccService.markCanceling(order.getId());
        afterCommit(() -> {
            try {
                couponTccService.cancel(order.getId(), order.getUserCouponId(), now);
            } catch (RuntimeException exception) {
                log.error("订单已关闭但优惠券 Cancel 未完成, orderId={}", order.getId(), exception);
            }
        });
    }

    private void restoreCouponAfterCommit(BookingOrder order, LocalDateTime now) {
        if (order.getUserCouponId() == null) {
            return;
        }
        afterCommit(() -> {
            try {
                couponTccService.restoreAfterRefund(order.getId(), order.getUserCouponId(), now);
            } catch (RuntimeException exception) {
                log.error("订单退款完成但优惠券恢复未完成, orderId={}", order.getId(), exception);
            }
        });
    }

    private void afterCommit(Runnable operation) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            operation.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                operation.run();
            }
        });
    }

    /**
     * 详情头查询已经带上游客或景点范围；查询不到时统一返回 NOT_FOUND，
     * 既符合接口语义，也不会向调用者暴露订单是否属于其他账号。
     */
    private OrderDetailVO completeOrderDetail(OrderDetailVO detail, Long orderId) {
        if (detail == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        detail.setItems(bookingMapper.listOrderItems(orderId));
        return detail;
    }

    /** 将 PageHelper 的分页对象转换为项目统一分页响应。 */
    private PageResult<OrderSummaryVO> toPageResult(Page<OrderSummaryVO> page) {
        return new PageResult<>(
                page.getResult(), page.getTotal(), page.getPageNum(), page.getPageSize());
    }
}
