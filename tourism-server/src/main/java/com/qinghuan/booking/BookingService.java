package com.qinghuan.booking;

import com.qinghuan.pojo.dto.OrderCreateDTO;
import com.qinghuan.pojo.dto.OrderPageQueryDTO;
import com.qinghuan.pojo.dto.VenueOrderPageQueryDTO;
import com.qinghuan.pojo.entity.BookingOrder;
import com.qinghuan.pojo.vo.OrderCreatedVO;
import com.qinghuan.pojo.vo.OrderDetailVO;
import com.qinghuan.pojo.vo.OrderSummaryVO;
import com.qinghuan.pojo.vo.PageResult;

import java.util.List;

public interface BookingService {

    /** 创建当前登录游客的订单，并完成库存预占。 */
    public OrderCreatedVO createOrder(OrderCreateDTO createOrderDTO);

    /** 分页查询当前登录游客自己的订单。 */
    PageResult<OrderSummaryVO> pageMyOrders(OrderPageQueryDTO queryDTO);

    /** 获取当前登录游客自己的订单详情。 */
    OrderDetailVO getMyOrder(Long orderId);

    /** 分页查询当前登录账号所属景点的订单。 */
    PageResult<OrderSummaryVO> pageVenueOrders(VenueOrderPageQueryDTO queryDTO);

    /** 获取当前登录账号所属景点的订单详情。 */
    OrderDetailVO getVenueOrder(Long orderId);

    /**
     * 模拟支付当前游客的待支付订单。
     *
     * @return true 表示支付成功；false 表示订单已超时并完成关闭
     */
    boolean payOrder(Long orderId);

    // 整单退款
    void refundOrder(Long orderId);

    // 取消待支付订单
    void cancelOrder(Long orderId);

    // 根据订单id取消超时订单
    void cancelTimeoutOrder(Long id);

    List<BookingOrder> listTimeoutOrders();

    /** 定时完成所属场次已经结束的已支付订单。 */
    int completePaidOrders(java.time.LocalDateTime now);
}
