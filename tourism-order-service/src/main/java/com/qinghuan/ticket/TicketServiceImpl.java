package com.qinghuan.ticket;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qinghuan.auth.context.UserContext;
import com.qinghuan.common.exception.BusinessException;
import com.qinghuan.common.exception.ErrorCode;
import com.qinghuan.pojo.dto.TicketPageQueryDTO;
import com.qinghuan.pojo.entity.Ticket;
import com.qinghuan.pojo.vo.PageResult;
import com.qinghuan.pojo.vo.TicketVO;
import com.qinghuan.pojo.vo.TicketVerificationInfo;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/** 负责校验电子票批量生成结果。事务由调用方的订单流程统一管理。 */
@Service
public class TicketServiceImpl implements TicketService {

    private final TicketMapper ticketMapper;

    public TicketServiceImpl(TicketMapper ticketMapper) {
        this.ticketMapper = ticketMapper;
    }

    @Override
    public void createTicketsForOrder(Long orderId, int quantity) {
        int insertedRows = ticketMapper.insertTicketsForOrder(orderId);
        // 一条订单明细必须对应一张票，数量不一致时让整个订单事务回滚。
        if (insertedRows != quantity) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "订单票券生成失败");
        }
    }

    @Override
    public List<Ticket> listTicketsByOrderId(Long orderId) {
        return ticketMapper.listTicketsByOrderId(orderId);
    }

    @Override
    public Integer updateTickets(List<Ticket> tickets) {
        return  ticketMapper.updateTickets(tickets);
    }

    @Override
    public int markRefunding(Long orderId) {
        return ticketMapper.markRefundingByOrderId(orderId);
    }

    @Override
    public int completeRefund(Long orderId) {
        return ticketMapper.voidRefundingByOrderId(orderId);
    }

    @Override
    public int cancelRefund(Long orderId, LocalDateTime now) {
        return ticketMapper.restoreRefundingByOrderId(orderId, now);
    }

    @Override
    public PageResult<TicketVO> pageMyTickets(TicketPageQueryDTO queryDTO) {
        PageHelper.startPage(queryDTO.getPage(), queryDTO.getSize());
        Page<TicketVO> page = (Page<TicketVO>) ticketMapper.listMyTickets(
                UserContext.getRequired().userId(), queryDTO);
        return new PageResult<>(
                page.getResult(), page.getTotal(), page.getPageNum(), page.getPageSize());
    }

    @Override
    public TicketVO getMyTicket(Long ticketId) {
        TicketVO ticket = ticketMapper.findMyTicket(
                ticketId, UserContext.getRequired().userId());
        if (ticket == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "票券不存在");
        }
        return ticket;
    }

    @Override
    public TicketVerificationInfo findForVerification(String ticketCode, Long venueId) {
        return ticketMapper.findForVerificationByCode(ticketCode, venueId);
    }

    @Override
    public TicketVerificationInfo findForVerification(Long ticketId, Long venueId) {
        return ticketMapper.findForVerificationById(ticketId, venueId);
    }

    @Override
    public int markUsed(Long ticketId, LocalDateTime verifiedAt) {
        return ticketMapper.markUsed(ticketId, verifiedAt);
    }

    @Override
    public int expireTickets(LocalDateTime now) {
        return ticketMapper.expireTickets(now);
    }
}
