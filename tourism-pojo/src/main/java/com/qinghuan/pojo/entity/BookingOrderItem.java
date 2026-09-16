package com.qinghuan.pojo.entity;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 下单时的参观人、票种和价格快照，创建后原则上不再修改。
 */
@Getter
@Setter
public class BookingOrderItem {

    private Long id;
    private Long orderId;
    private Long visitorId;
    private Long sessionTicketTypeId;
    private String visitorName;
    private String visitorIdType;
    private String visitorIdNumber;
    /** 证件指纹用于同一场次的一人一单判断，不再查询用户服务的 visitor_identity。 */
    private String visitorFingerprint;
    private String ticketTypeName;
    private BigDecimal unitPrice;
    private LocalDateTime createdAt;
}
