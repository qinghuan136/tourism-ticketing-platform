package com.qinghuan.session;

import com.qinghuan.pojo.entity.InventoryReservation;
import com.qinghuan.pojo.entity.InventoryReservationOrder;
import com.qinghuan.pojo.enums.InventoryReservationStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 库存 TCC 预留记录的持久化访问。 */
@Mapper
public interface InventoryReservationMapper {

    int insertOrderIfAbsent(@Param("orderId") Long orderId,
                            @Param("sessionId") Long sessionId,
                            @Param("status") InventoryReservationStatus status);

    InventoryReservationOrder findOrderForUpdate(Long orderId);

    List<InventoryReservation> listReservationsForUpdate(Long orderId);

    int insertReservation(@Param("orderId") Long orderId,
                          @Param("sessionTicketTypeId") Long sessionTicketTypeId,
                          @Param("quantity") Integer quantity,
                          @Param("status") InventoryReservationStatus status);

    int updateOrderStatus(@Param("orderId") Long orderId,
                          @Param("status") InventoryReservationStatus status);

    int updateReservationStatus(@Param("orderId") Long orderId,
                                @Param("status") InventoryReservationStatus status);

    int insertCancelMarkerIfAbsent(Long orderId);
}
