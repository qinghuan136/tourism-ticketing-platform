package com.qinghuan.booking;

import com.qinghuan.pojo.entity.BookingCouponTccOperation;
import com.qinghuan.pojo.enums.BookingCouponTccStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 订单侧优惠券 TCC 协调记录。 */
@Mapper
public interface BookingCouponTccMapper {

    int insert(@Param("orderId") Long orderId,
               @Param("couponId") Long couponId,
               @Param("status") BookingCouponTccStatus status);

    int updateStatus(@Param("orderId") Long orderId,
                     @Param("oldStatus") BookingCouponTccStatus oldStatus,
                     @Param("newStatus") BookingCouponTccStatus newStatus);

    List<BookingCouponTccOperation> listPending();
}
