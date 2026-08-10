package com.qinghuan.session;

import com.qinghuan.pojo.dto.SessionPageQueryDTO;
import com.qinghuan.pojo.dto.SessionTicketTypeConfigDTO;
import com.qinghuan.pojo.entity.AdmissionSession;
import com.qinghuan.pojo.entity.SessionTicketType;
import com.qinghuan.pojo.enums.AdmissionSessionStatus;
import com.qinghuan.pojo.vo.SessionInventorySnapshotVO;
import com.qinghuan.pojo.vo.SessionStaticSnapshotVO;
import com.qinghuan.pojo.vo.SessionTicketTypeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SessionMapper {

    /**
     * 按日期和状态筛选指定景点的场次主体，供分页使用。
     */
    List<AdmissionSession> list(@Param("venueId") Long venueId,
                                @Param("query") SessionPageQueryDTO queryDTO);

    /**
     * 按场次 ID 和景点 ID 查询，作为所有单场次操作的范围约束。
     */
    AdmissionSession findById(@Param("id") Long id,
                              @Param("venueId") Long venueId);

    /** 按场次 ID 查询订单业务所需的场次信息，不读取登录账号的景点范围。 */
    AdmissionSession findByIdForOrder(Long id);

    /**
     * 查询场次已配置的票种，并关联返回票种名称。
     */
    List<SessionTicketTypeVO> listTicketTypes(Long sessionId);

    /**
     * 查询当前仍可预约的场次票种，供订单校验、计价和生成快照。
     */
    List<SessionTicketTypeVO> listOrderableTicketTypes(
            @Param("sessionId") Long sessionId,
            @Param("ids") List<Long> sessionTicketTypeIds);

    /**
     * 创建订单时原子扣减场次总容量。
     */
    int deductSessionCapacity(@Param("sessionId") Long sessionId,
                              @Param("quantity") int quantity);

    /**
     * 创建订单时原子扣减单个场次票种的剩余数量。
     */
    int deductTicketTypeQuantity(@Param("sessionId") Long sessionId,
                                 @Param("sessionTicketTypeId") Long sessionTicketTypeId,
                                 @Param("quantity") int quantity);

    /**
     * 订单取消、超时关闭或退款时归还场次总容量。
     */
    int restoreSessionCapacity(@Param("sessionId") Long sessionId,
                               @Param("quantity") int quantity);

    /**
     * 订单取消、超时关闭或退款时归还场次票种数量。
     */
    int restoreTicketTypeQuantity(@Param("sessionId") Long sessionId,
                                  @Param("sessionTicketTypeId") Long sessionTicketTypeId,
                                  @Param("quantity") int quantity);

    /**
     * 统计当前景点内处于 ENABLED 状态的指定票种数量。
     */
    int countEnabledTicketTypes(@Param("venueId") Long venueId,
                                @Param("ids") List<Long> ticketTypeIds);

    /**
     * 写入场次主体，并回填数据库生成的场次 ID。
     */
    int insertSession(AdmissionSession session);

    /**
     * 批量写入同一场次的票种售价与配额配置。
     */
    void insertSessionTicketTypes(List<SessionTicketType> ticketTypes);

    /**
     * 仅更新当前景点的草稿场次，避免修改已经开放的场次。
     */
    int updateDraftSession(@Param("session") AdmissionSession session,
                           @Param("venueId") Long venueId);

    /**
     * 判断场次是否存在可售票种，且全部票种配额未超出场次容量。
     */
    boolean isOpenable(@Param("sessionId") Long sessionId,
                       @Param("totalCapacity") Integer totalCapacity);

    /**
     * 统计未取消、未关闭的订单，用于取消场次前的业务校验。
     */
    int countUnresolvedOrders(Long sessionId);

    /**
     * 在旧状态仍匹配时更新状态，防止并发事件覆盖。
     */
    int updateStatus(@Param("id") Long id,
                     @Param("venueId") Long venueId,
                     @Param("currentStatus") AdmissionSessionStatus currentStatus,
                     @Param("targetStatus") AdmissionSessionStatus targetStatus);

    /**
     * 删除场次下的全部票种配置。
     */
    void deleteSessionTicketTypes(Long sessionId);

    /**
     * 统计场次的全部订单，用于物理删除前校验。
     */
    int countOrders(Long sessionId);

    /**
     * 仅删除当前景点仍为草稿状态的场次主体。
     */
    int deleteDraftSession(@Param("id") Long id,
                           @Param("venueId") Long venueId);

    /**
     * 查询一个开放场次及其票种静态信息。
     *
     * 不查询任何剩余容量和剩余库存字段。
     */
    SessionStaticSnapshotVO findStaticSnapshot(Long sessionId);
    /**
     * 批量查询正在预约窗口内的场次库存。
     */
    List<SessionInventorySnapshotVO> listInventorySnapshots(
            @Param("sessionIds") List<Long> sessionIds,
            @Param("now") LocalDateTime now
    );

    /**
     * 查询使用指定基础票种的场次，供 Canal 精确清理场次静态缓存。
     */
    List<Long> listSessionIdsByTicketTypeId(
            @Param("ticketTypeId") Long ticketTypeId
    );

    /** 将预约窗口已结束的开放场次收口为 CLOSED。 */
    int closeExpiredBookings(@Param("now") LocalDateTime now);

    /** 将参观时间已结束的开放或关闭场次收口为 ENDED。 */
    int endExpiredSessions(@Param("now") LocalDateTime now);
}
