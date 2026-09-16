package com.qinghuan.booking;

import com.qinghuan.annotation.RequireRole;
import com.qinghuan.common.exception.BusinessException;
import com.qinghuan.common.exception.ErrorCode;
import com.qinghuan.common.response.ApiResponse;
import com.qinghuan.pojo.dto.OrderCreateDTO;
import com.qinghuan.pojo.dto.OrderPageQueryDTO;
import com.qinghuan.pojo.dto.VenueOrderPageQueryDTO;
import com.qinghuan.pojo.enums.AccountRole;
import com.qinghuan.pojo.vo.OrderCreatedVO;
import com.qinghuan.pojo.vo.OrderDetailVO;
import com.qinghuan.pojo.vo.OrderSummaryVO;
import com.qinghuan.pojo.vo.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@Tag(name = "订单管理", description = "游客下单与订单生命周期、运营端订单查询")
@SecurityRequirement(name = "BearerAuth")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /** 创建订单并预占场次与票种库存。 */
    @PostMapping("/tourist/orders")
    @ResponseStatus(HttpStatus.CREATED)
    @RequireRole(AccountRole.TOURIST)
    @Operation(summary = "创建订单", description = "为一个或多个参观人预占指定场次票种库存；同一参观人在同一场次只能存在一笔有效订单")
    public ApiResponse<OrderCreatedVO> createOrder(
            @Valid @RequestBody OrderCreateDTO orderCreateDTO) {
        log.info("创建订单：{}", orderCreateDTO);
        return ApiResponse.success(bookingService.createOrder(orderCreateDTO));
    }

    /** 分页查询当前游客自己的订单，不能通过参数查询其他游客的数据。 */
    @GetMapping("/tourist/orders")
    @RequireRole(AccountRole.TOURIST)
    @Operation(summary = "分页查询我的订单")
    public ApiResponse<PageResult<OrderSummaryVO>> pageMyOrders(
            @ParameterObject @Valid OrderPageQueryDTO queryDTO) {
        return ApiResponse.success(bookingService.pageMyOrders(queryDTO));
    }

    /** 查询当前游客自己的订单详情及电子票信息。 */
    @GetMapping("/tourist/orders/{orderId}")
    @RequireRole(AccountRole.TOURIST)
    @Operation(summary = "获取我的订单详情")
    public ApiResponse<OrderDetailVO> getMyOrder(
            @PathVariable @Positive(message = "订单ID必须为正数") Long orderId) {
        return ApiResponse.success(bookingService.getMyOrder(orderId));
    }

    /** MVP 阶段使用后端模拟支付，支付成功后直接生成电子票。 */
    @PostMapping("/tourist/orders/{orderId}/pay")
    @RequireRole(AccountRole.TOURIST)
    @Operation(summary = "支付订单")
    public ApiResponse<Void> payOrder(
            @PathVariable @Positive(message = "订单ID必须为正数") Long orderId) {
        boolean paid = bookingService.payOrder(orderId);
        if (!paid) {
            // Service 已提交超时关闭事务，此处只负责转换成接口冲突响应。
            throw new BusinessException(ErrorCode.CONFLICT, "订单已超时，无法支付");
        }
        return ApiResponse.success();
    }

    /** 运营者和工作人员按当前景点范围分页查询订单。 */
    @GetMapping("/operator/orders")
    @RequireRole({AccountRole.OPERATOR, AccountRole.STAFF})
    @Operation(summary = "运营端分页查询订单")
    public ApiResponse<PageResult<OrderSummaryVO>> pageVenueOrders(
            @ParameterObject @Valid VenueOrderPageQueryDTO queryDTO) {
        return ApiResponse.success(bookingService.pageVenueOrders(queryDTO));
    }

    /** 查询当前账号所属景点的订单详情，跨景点订单统一按不存在处理。 */
    @GetMapping("/operator/orders/{orderId}")
    @RequireRole({AccountRole.OPERATOR, AccountRole.STAFF})
    @Operation(summary = "运营端获取订单详情")
    public ApiResponse<OrderDetailVO> getVenueOrder(
            @PathVariable @Positive(message = "订单ID必须为正数") Long orderId) {
        return ApiResponse.success(bookingService.getVenueOrder(orderId));
    }

    /*
     * 整单退款
     */
    @PostMapping("/tourist/orders/{orderId}/refund")
    @RequireRole(AccountRole.TOURIST)
    @Operation(
            summary = "整单退款",
            description = "接受退款申请并通过 REFUNDING 中间状态处理平台退款；超时结果由后台对账")
    public ApiResponse<Void> refundOrder(
            @PathVariable @Positive(message = "订单ID必须为正数") Long orderId) {
        bookingService.refundOrder(orderId);
        return ApiResponse.success();
    }

    /*
     * 取消支付订单
     */
    @PostMapping("/tourist/orders/{orderId}/cancel")
    @RequireRole(AccountRole.TOURIST)
    @Operation(summary = "取消支付订单")
    public ApiResponse<Void> cancelOrder(
            @PathVariable @Positive(message = "订单ID必须为正数") Long orderId) {
        bookingService.cancelOrder(orderId);
        return ApiResponse.success();
    }
}
