package com.qinghuan.session;

import com.qinghuan.pojo.vo.SessionInventorySnapshotVO;
import com.qinghuan.pojo.vo.SessionTicketTypeVO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 提供给订单模块使用的场次票种查询和库存能力。
 * session_ticket_type 是场次的子资源，因此不单独建立业务模块。
 */
public interface SessionInventoryService {

    /**
     * 查询属于目标场次且当前可售的场次票种，用于后端计价和生成订单快照。
     */
    List<SessionTicketTypeVO> getOrderableTicketTypes(
            Long sessionId, List<Long> sessionTicketTypeIds);

    /**
     * 原子扣减场次容量和各票种数量。
     * 订单创建通过库存 TCC 调用该能力；普通订单取消、退款仍使用对应归还能力。
     */
    void reserveInventory(Long sessionId, Map<Long, Integer> ticketTypeQuantities);

    /**
     * 订单取消、超时关闭或退款时归还对应库存。
     */
    void releaseInventory(Long sessionId, Map<Long, Integer> ticketTypeQuantities);

    /**
     * 批量查询正在销售场次的动态库存。
     */
    List<SessionInventorySnapshotVO> listInventorySnapshots(
            List<Long> sessionIds,
            LocalDateTime now
    );
}
