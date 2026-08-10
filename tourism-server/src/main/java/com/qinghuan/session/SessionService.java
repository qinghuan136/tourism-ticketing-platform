package com.qinghuan.session;

import com.qinghuan.pojo.dto.SessionPageQueryDTO;
import com.qinghuan.pojo.dto.SessionWriteDTO;
import com.qinghuan.pojo.enums.SessionEvent;
import com.qinghuan.pojo.vo.PageResult;
import com.qinghuan.pojo.vo.SessionStaticSnapshotVO;
import com.qinghuan.pojo.vo.SessionVO;

import java.time.LocalDateTime;
import java.util.List;

public interface SessionService {

    /**
     * 分页返回当前景点的场次及其票种配置。
     */
    PageResult<SessionVO> pageSessions(SessionPageQueryDTO queryDTO);

    /**
     * 新建 DRAFT 场次，并初始化场次和票种的剩余数量。
     */
    Long createSession(SessionWriteDTO writeDTO);

    /**
     * 查询当前景点内指定场次的完整信息。
     */
    SessionVO getSession(Long sessionId);

    /**
     * 返回订单业务校验所需的场次开始时间，不受运营端景点数据范围影响。
     */
    LocalDateTime getSessionStartTime(Long sessionId);

    /** 返回订单及优惠券校验所需的场次所属景点。 */
    Long getSessionVenueId(Long sessionId);

    /**
     * 修改草稿场次，并整组替换票种售价和配额。
     */
    void updateDraftSession(Long sessionId, SessionWriteDTO writeDTO);

    /**
     * 按当前状态和业务事件执行一次状态流转。
     */
    void handleSessionEvent(Long sessionId, SessionEvent event);

    /**
     * 删除没有订单的草稿场次及其票种配置。
     */
    void deleteDraftSession(Long sessionId);

    /**
     * 通过Caffeine、Redis、MySQL查询场次票种静态快照。
     */
    SessionStaticSnapshotVO getSessionStaticSnapshot(Long sessionId);

    /**
     * 删除指定场次的Redis和Caffeine静态快照。
     *
     * 供场次写操作、票种写操作和后续Canal消费端调用。
     */
    void evictSessionStaticSnapshot(Long sessionId);

    /**
     * 查询使用指定基础票种的场次，供 Canal 定位需要失效的缓存。
     */
    List<Long> listSessionIdsByTicketTypeId(Long ticketTypeId);

    /** 定时收口预约结束和参观结束的场次状态。 */
    int maintainLifecycle(LocalDateTime now);

}
