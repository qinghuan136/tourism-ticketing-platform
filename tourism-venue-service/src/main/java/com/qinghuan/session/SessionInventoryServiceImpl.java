package com.qinghuan.session;

import com.qinghuan.common.exception.BusinessException;
import com.qinghuan.common.exception.ErrorCode;
import com.qinghuan.pojo.vo.SessionInventorySnapshotVO;
import com.qinghuan.pojo.vo.SessionTicketTypeVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class SessionInventoryServiceImpl implements SessionInventoryService {

    private final SessionMapper sessionMapper;

    public SessionInventoryServiceImpl(SessionMapper sessionMapper) {
        this.sessionMapper = sessionMapper;
    }

    @Override
    public List<SessionTicketTypeVO> getOrderableTicketTypes(
            Long sessionId, List<Long> sessionTicketTypeIds) {
        List<Long> uniqueIds = sessionTicketTypeIds.stream().distinct().toList();
        if (uniqueIds.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "订单至少需要选择一个场次票种");
        }

        List<SessionTicketTypeVO> ticketTypes =
                sessionMapper.listOrderableTicketTypes(sessionId, uniqueIds);
        if (ticketTypes.size() != uniqueIds.size()) {
            throw new BusinessException(
                    ErrorCode.CONFLICT, "场次未开放或存在不可售的场次票种");
        }
        return ticketTypes;
    }

    @Override
    @Transactional
    public void reserveInventory(
            Long sessionId, Map<Long, Integer> ticketTypeQuantities) {
        int totalQuantity = totalQuantity(ticketTypeQuantities);
        if (sessionMapper.deductSessionCapacity(sessionId, totalQuantity) == 0) {
            throw new BusinessException(
                    ErrorCode.CONFLICT, "场次未开放、预约时间已结束或容量不足");
        }

        // 固定扣减顺序，减少多个订单同时购买不同票种时发生死锁的概率。
        for (Map.Entry<Long, Integer> entry : sortedEntries(ticketTypeQuantities)) {
            int updatedRows = sessionMapper.deductTicketTypeQuantity(
                    sessionId, entry.getKey(), entry.getValue());
            if (updatedRows == 0) {
                throw new BusinessException(ErrorCode.CONFLICT, "场次票种已停售或余量不足");
            }
        }
    }

    @Override
    @Transactional
    public void releaseInventory(
            Long sessionId, Map<Long, Integer> ticketTypeQuantities) {
        int totalQuantity = totalQuantity(ticketTypeQuantities);
        if (sessionMapper.restoreSessionCapacity(sessionId, totalQuantity) == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "场次容量不能重复归还");
        }

        for (Map.Entry<Long, Integer> entry : sortedEntries(ticketTypeQuantities)) {
            int updatedRows = sessionMapper.restoreTicketTypeQuantity(
                    sessionId, entry.getKey(), entry.getValue());
            if (updatedRows == 0) {
                throw new BusinessException(ErrorCode.CONFLICT, "场次票种数量不能重复归还");
            }
        }
    }

    @Override
    public List<SessionInventorySnapshotVO> listInventorySnapshots(
            List<Long> sessionIds,
            LocalDateTime now) {

        // MyBatis不能生成空的IN条件。
        if (sessionIds.isEmpty()) {
            return List.of();
        }

        return sessionMapper.listInventorySnapshots(
                sessionIds,
                now
        );
    }

    /**
     * 库存数量必须为正数；同时在这里计算订单占用的场次总容量。
     */
    private int totalQuantity(Map<Long, Integer> ticketTypeQuantities) {
        if (ticketTypeQuantities.isEmpty()
                || ticketTypeQuantities.values().stream().anyMatch(quantity -> quantity == null || quantity <= 0)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "票种购买数量必须大于0");
        }
        return ticketTypeQuantities.values().stream().mapToInt(Integer::intValue).sum();
    }

    private List<Map.Entry<Long, Integer>> sortedEntries(
            Map<Long, Integer> ticketTypeQuantities) {
        return ticketTypeQuantities.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .toList();
    }
}

