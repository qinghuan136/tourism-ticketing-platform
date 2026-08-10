package com.qinghuan.ticket;

import com.qinghuan.pojo.dto.TicketPageQueryDTO;
import com.qinghuan.pojo.entity.Ticket;
import com.qinghuan.pojo.vo.TicketVO;
import com.qinghuan.pojo.vo.TicketVerificationInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TicketMapper {

    /** 根据订单明细批量生成电子票，有效期直接取订单对应的场次时间。 */
    int insertTicketsForOrder(Long orderId);

    /** 根据订单ID查询所属票券。 */
    List<Ticket> listTicketsByOrderId(Long orderId);

    /** 批量修改票券 */
    Integer updateTickets(@Param("tickets") List<Ticket> tickets);

    /** 按当前游客和状态查询电子票，供 PageHelper 分页。 */
    List<TicketVO> listMyTickets(@Param("userId") Long userId,
                                 @Param("query") TicketPageQueryDTO queryDTO);

    /** 查询当前游客自己的电子票详情。 */
    TicketVO findMyTicket(@Param("ticketId") Long ticketId,
                          @Param("userId") Long userId);

    /** 按票码和景点查找核销所需信息。 */
    TicketVerificationInfo findForVerificationByCode(@Param("ticketCode") String ticketCode,
                                                     @Param("venueId") Long venueId);

    /** 按票券 ID 和景点查找核销所需信息。 */
    TicketVerificationInfo findForVerificationById(@Param("ticketId") Long ticketId,
                                                   @Param("venueId") Long venueId);

    /** 核销时使用状态和有效期条件更新，数据库负责裁决并发请求。 */
    int markUsed(@Param("ticketId") Long ticketId,
                 @Param("verifiedAt") LocalDateTime verifiedAt);

    /** 将超过有效期但仍未使用的票券收口为 EXPIRED。 */
    int expireTickets(@Param("now") LocalDateTime now);
}
