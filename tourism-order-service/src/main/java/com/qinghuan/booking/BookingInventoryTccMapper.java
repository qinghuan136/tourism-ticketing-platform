package com.qinghuan.booking;

import com.qinghuan.pojo.entity.BookingInventoryTccOperation;
import com.qinghuan.pojo.enums.BookingInventoryTccStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 订单侧库存 TCC 协调记录。 */
@Mapper
public interface BookingInventoryTccMapper {

    int insert(@Param("orderId") Long orderId,
               @Param("sessionId") Long sessionId,
               @Param("status") BookingInventoryTccStatus status);

    int updateStatus(@Param("orderId") Long orderId,
                     @Param("oldStatus") BookingInventoryTccStatus oldStatus,
                     @Param("newStatus") BookingInventoryTccStatus newStatus);

    List<BookingInventoryTccOperation> listPending();
}
