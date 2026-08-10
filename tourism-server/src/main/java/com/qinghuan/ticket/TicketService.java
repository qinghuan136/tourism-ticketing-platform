package com.qinghuan.ticket;

import com.qinghuan.pojo.entity.Ticket;
import com.qinghuan.pojo.dto.TicketPageQueryDTO;
import com.qinghuan.pojo.vo.PageResult;
import com.qinghuan.pojo.vo.TicketVO;
import com.qinghuan.pojo.vo.TicketVerificationInfo;

import java.time.LocalDateTime;
import java.util.List;

/** 订单支付完成后的电子票生成能力。 */
public interface TicketService {

    /**
     * 为已完成支付的订单明细生成一人一票。
     * quantity 用来核对实际生成数量，防止订单明细缺失却静默成功。
     */
    void createTicketsForOrder(Long orderId, int quantity);
    /**
     * 根据订单id查询所有所属票券
     */
    List<Ticket> listTicketsByOrderId(Long orderId);
    /*
     * 批量修改票券
     */
    Integer updateTickets(List<Ticket> tickets);

    /** 分页查询当前游客自己的电子票。 */
    PageResult<TicketVO> pageMyTickets(TicketPageQueryDTO queryDTO);

    /** 获取当前游客自己的电子票详情。 */
    TicketVO getMyTicket(Long ticketId);

    /** 按当前景点范围读取核销所需票券信息。 */
    TicketVerificationInfo findForVerification(String ticketCode, Long venueId);

    /** 按票券 ID 和景点范围读取核销信息，用于返回幂等请求的原结果。 */
    TicketVerificationInfo findForVerification(Long ticketId, Long venueId);

    /** 在票券仍有效且处于有效时间内时原子更新为已使用。 */
    int markUsed(Long ticketId, LocalDateTime verifiedAt);

    /** 定时将超过有效期的未使用票券改为 EXPIRED。 */
    int expireTickets(LocalDateTime now);
}
