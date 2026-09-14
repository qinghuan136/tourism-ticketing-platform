package com.qinghuan.booking;

import com.qinghuan.pojo.dto.VenueOrderPageQueryDTO;
import com.qinghuan.pojo.entity.BookingOrder;
import com.qinghuan.pojo.entity.BookingOrderItem;
import com.qinghuan.pojo.enums.BookingOrderStatus;
import com.qinghuan.pojo.vo.OrderDetailVO;
import com.qinghuan.pojo.vo.OrderItemVO;
import com.qinghuan.pojo.vo.OrderSummaryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BookingMapper {

    /** 创建订单；生成的主键通过 MyBatis 写回 bookingOrder.id。 */
    int insertOrder(BookingOrder bookingOrder);

    /** 批量保存下单时的参观人、证件、票种和成交价格快照。 */
    void insertOrderItems(@Param("items") List<BookingOrderItem> orderItems);

    /** 按游客归属查询订单摘要，供 PageHelper 分页。 */
    List<OrderSummaryVO> listMyOrders(@Param("userId") Long userId,
                                     @Param("status") BookingOrderStatus status);

    /** 按景点范围和运营端筛选条件查询订单摘要。 */
    List<OrderSummaryVO> listVenueOrders(
            @Param("venueId") Long venueId,
            @Param("query") VenueOrderPageQueryDTO queryDTO);

    /** 查询属于指定游客的订单详情头。 */
    OrderDetailVO findMyOrderDetail(@Param("orderId") Long orderId,
                                    @Param("userId") Long userId);

    /** 查询属于指定景点的订单详情头，并补充购买人概要。 */
    OrderDetailVO findVenueOrderDetail(@Param("orderId") Long orderId,
                                       @Param("venueId") Long venueId);

    /** 一次查询订单全部明细及其可能存在的电子票，避免逐条查询票券。 */
    List<OrderItemVO> listOrderItems(Long orderId);

    /** 按订单号查询订单基础信息。 */
    BookingOrder findOrderByOrderId(Long id);

    /** 查询指定场次中与证件身份冲突的有效订单，用于一人一场次校验。 */
    List<BookingOrder> findConflictingOrdersBySessionAndFingerprints(
            @Param("sessionId") Long sessionId,
            @Param("fingerprints") List<String> fingerprints);

    /** 更新订单信息。 */

    Integer updateOrder(@Param("order") BookingOrder order,
                        @Param("oldStatus") BookingOrderStatus oldStatus);

    /** 仅在订单仍待支付且未超时时完成支付状态更新。 */
    int updatePaidOrder(@Param("order") BookingOrder order,
                        @Param("oldStatus") BookingOrderStatus oldStatus);

    /** 第三方明确退款失败时，恢复已支付状态并清理本次退款单号。 */
    int resetFailedRefund(@Param("orderId") Long orderId,
                          @Param("newStatus") BookingOrderStatus newStatus);

    // 列出所有超时订单
    List<BookingOrder> listTimeoutOrders(LocalDateTime timeout);

    /** 扫描超过等待时间仍未确认结果的退款订单。 */
    List<BookingOrder> listRefundingOrders(LocalDateTime requestedBefore);

    /** 场次结束后将已支付订单收口为 COMPLETED。 */
    int completePaidOrders(@Param("now") LocalDateTime now);
}
