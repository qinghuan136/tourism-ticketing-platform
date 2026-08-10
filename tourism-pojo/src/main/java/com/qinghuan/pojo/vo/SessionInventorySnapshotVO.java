package com.qinghuan.pojo.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 场次当前动态库存。
 *
 * 该对象不能放入Caffeine。
 */
@Getter
@Setter
public class SessionInventorySnapshotVO {

    private Long sessionId;
    private Integer remainingCapacity;
    private List<SessionTicketTypeInventoryVO> ticketTypes;
}