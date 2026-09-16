package com.qinghuan.session;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qinghuan.auth.context.UserContext;
import com.qinghuan.common.constant.cacheKeys.SessionCacheConstant;
import com.qinghuan.common.exception.BusinessException;
import com.qinghuan.common.exception.ErrorCode;
import com.qinghuan.pojo.dto.SessionPageQueryDTO;
import com.qinghuan.pojo.dto.SessionTicketTypeConfigDTO;
import com.qinghuan.pojo.dto.SessionWriteDTO;
import com.qinghuan.pojo.entity.AdmissionSession;
import com.qinghuan.pojo.entity.SessionTicketType;
import com.qinghuan.pojo.enums.AdmissionSessionStatus;
import com.qinghuan.pojo.enums.SaleStatus;
import com.qinghuan.pojo.enums.SessionEvent;
import com.qinghuan.pojo.vo.PageResult;
import com.qinghuan.pojo.vo.SessionStaticSnapshotVO;
import com.qinghuan.pojo.vo.SessionTicketTypeStaticVO;
import com.qinghuan.pojo.vo.SessionVO;
import com.qinghuan.redis.CacheClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class SessionServiceImpl implements SessionService {

    private final SessionMapper sessionMapper;
    private final OrderClient orderClient;
    private final CacheClient cacheClient;

    private final Cache<Long, SessionStaticSnapshotVO>
            sessionStaticLocalCache;

    public SessionServiceImpl(
            SessionMapper sessionMapper,
            OrderClient orderClient,
            CacheClient cacheClient,
            @Qualifier("sessionStaticLocalCache")
            Cache<Long, SessionStaticSnapshotVO> sessionStaticLocalCache) {
        this.sessionMapper = sessionMapper;
        this.orderClient = orderClient;
        this.cacheClient = cacheClient;
        this.sessionStaticLocalCache = sessionStaticLocalCache;
    }

    /**
     * 分页查询场次主体，并补充每个场次的票种配置。
     */
    @Override
    public PageResult<SessionVO> pageSessions(SessionPageQueryDTO queryDTO) {
        Long venueId = UserContext.getRequired().venueId();
        PageHelper.startPage(queryDTO.getPage(), queryDTO.getSize());
        Page<AdmissionSession> page = (Page<AdmissionSession>) sessionMapper.list(venueId, queryDTO);

        List<SessionVO> items = page.getResult().stream()
                .map(this::toVO)
                .toList();
        return new PageResult<>(items, page.getTotal(), page.getPageNum(), page.getPageSize());
    }

    /**
     * 创建草稿场次，容量和票种余量均从本次配置初始化。
     */
    @Override
    @Transactional
    public Long createSession(SessionWriteDTO writeDTO) {
        Long venueId = UserContext.getRequired().venueId();
        validateWriteRequest(writeDTO, venueId);

        AdmissionSession session = buildSession(writeDTO, venueId);
        try {
            sessionMapper.insertSession(session);
            sessionMapper.insertSessionTicketTypes(buildTicketTypes(session.getId(), writeDTO));
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.CONFLICT, "当前景点已存在相同日期和时间段的场次");
        }
        return session.getId();
    }

    /**
     * 查询当前景点的场次详情。
     */
    @Override
    public SessionVO getSession(Long sessionId) {
        AdmissionSession session = getCurrentVenueSession(sessionId);
        return toVO(session);
    }

    @Override
    public LocalDateTime getSessionStartTime(Long sessionId) {
        AdmissionSession session = sessionMapper.findByIdForOrder(sessionId);
        if (session == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "场次不存在");
        }
        return LocalDateTime.of(session.getVisitDate(), session.getStartTime());
    }

    @Override
    public Long getSessionVenueId(Long sessionId) {
        AdmissionSession session = getSessionForOrder(sessionId);
        return session.getVenueId();
    }

    @Override
    public AdmissionSession getSessionForOrder(Long sessionId) {
        AdmissionSession session = sessionMapper.findByIdForOrder(sessionId);
        if (session == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "场次不存在");
        }
        return session;
    }

    /**
     * 草稿没有售票记录，因此允许整组重置票种配置和剩余量。
     */
    @Override
    @Transactional
    public void updateDraftSession(Long sessionId, SessionWriteDTO writeDTO) {
        AdmissionSession current = getCurrentVenueSession(sessionId);
        if (current.getStatus() != AdmissionSessionStatus.DRAFT) {
            throw new BusinessException(ErrorCode.CONFLICT, "只有草稿场次可以修改");
        }

        Long venueId = UserContext.getRequired().venueId();
        validateWriteRequest(writeDTO, venueId);
        AdmissionSession updatedSession = buildSession(writeDTO, venueId);
        updatedSession.setId(sessionId);

        try {
            int updatedRows = sessionMapper.updateDraftSession(updatedSession, venueId);
            if (updatedRows == 0) {
                throw new BusinessException(ErrorCode.CONFLICT, "只有草稿场次可以修改");
            }
            // 草稿尚未售票，可以直接整组替换票种配置并重置剩余量。
            sessionMapper.deleteSessionTicketTypes(sessionId);
            sessionMapper.insertSessionTicketTypes(buildTicketTypes(sessionId, writeDTO));
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.CONFLICT, "当前景点已存在相同日期和时间段的场次");
        }
    }

    /**
     * 根据当前状态和事件计算下一状态，再校验事件对应的业务条件。
     */
    @Override
    @Transactional
    public void handleSessionEvent(Long sessionId, SessionEvent event) {
        AdmissionSession session = getCurrentVenueSession(sessionId);
        AdmissionSessionStatus currentStatus = session.getStatus();
        AdmissionSessionStatus targetStatus;
        try {
            targetStatus = currentStatus.next(event);
        } catch (IllegalStateException exception) {
            throw new BusinessException(ErrorCode.CONFLICT, "当前场次状态不支持该事件");
        }

        validateEvent(session, event);
        // 更新条件携带原状态，避免并发事件互相覆盖。
        int updatedRows = sessionMapper.updateStatus(
                sessionId,
                UserContext.getRequired().venueId(),
                currentStatus,
                targetStatus
        );
        if (updatedRows == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "场次状态已发生变化，请刷新后重试");
        }
    }

    /**
     * 仅删除无订单的草稿，并按外键依赖顺序删除关联配置。
     */
    @Override
    @Transactional
    public void deleteDraftSession(Long sessionId) {
        AdmissionSession session = getCurrentVenueSession(sessionId);
        if (session.getStatus() != AdmissionSessionStatus.DRAFT) {
            throw new BusinessException(ErrorCode.CONFLICT, "只有草稿场次可以删除");
        }
        if (getSessionOrderState(sessionId).hasOrders()) {
            throw new BusinessException(ErrorCode.CONFLICT, "已有订单的场次不能删除");
        }

        // 关联表使用 RESTRICT 外键，因此先删除票种配置，再删除场次。
        sessionMapper.deleteSessionTicketTypes(sessionId);
        int deletedRows = sessionMapper.deleteDraftSession(
                sessionId, UserContext.getRequired().venueId());
        if (deletedRows == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "只有草稿场次可以删除");
        }
    }

    /**
     * 在当前景点范围内读取场次，避免跨景点访问。
     */
    private AdmissionSession getCurrentVenueSession(Long sessionId) {
        AdmissionSession session = sessionMapper.findById(
                sessionId, UserContext.getRequired().venueId());
        if (session == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "场次不存在");
        }
        return session;
    }

    /**
     * 将场次主体和票种配置组合为接口返回对象。
     */
    private SessionVO toVO(AdmissionSession session) {
        return SessionVO.from(session, sessionMapper.listTicketTypes(session.getId()));
    }

    /**
     * 校验事件的目标状态前置条件。
     */
    private void validateEvent(AdmissionSession session, SessionEvent event) {
        switch (event) {
            case PUBLISH, REOPEN_BOOKING -> validateCanOpen(session);
            case CANCEL -> {
                if (getSessionOrderState(session.getId()).hasUnresolvedOrders()) {
                    throw new BusinessException(ErrorCode.CONFLICT, "场次存在未处理订单，不能取消");
                }
            }
            case SESSION_ENDED -> {
                LocalDateTime sessionEnd = LocalDateTime.of(
                        session.getVisitDate(), session.getEndTime());
                if (LocalDateTime.now().isBefore(sessionEnd)) {
                    throw new BusinessException(ErrorCode.CONFLICT, "场次尚未结束");
                }
            }
            case CLOSE_BOOKING -> {
                // 关闭预约只阻止新订单，不改变已有订单和票种余量。
            }
        }
    }

    /**
     * 发布或重新开放前，必须仍在预约窗口内且存在可售票种。
     */
    private void validateCanOpen(AdmissionSession session) {
        if (!LocalDateTime.now().isBefore(session.getBookingEndAt())) {
            throw new BusinessException(ErrorCode.CONFLICT, "场次预约时间已经结束");
        }
        if (session.getRemainingCapacity() <= 0
                || !sessionMapper.isOpenable(session.getId(), session.getTotalCapacity())) {
            throw new BusinessException(ErrorCode.CONFLICT, "场次没有可售票种或票种配额不合法");
        }
    }

    private com.qinghuan.pojo.remote.order.SessionOrderStateDTO getSessionOrderState(Long sessionId) {
        return orderClient.getSessionOrderState(sessionId).data();
    }

    /**
     * 校验草稿写入数据的时间、配额和票种归属。
     */
    private void validateWriteRequest(SessionWriteDTO writeDTO, Long venueId) {
        if (!writeDTO.getStartTime().isBefore(writeDTO.getEndTime())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "场次开始时间必须早于结束时间");
        }
        if (!writeDTO.getBookingStartAt().isBefore(writeDTO.getBookingEndAt())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "预约开始时间必须早于结束时间");
        }
        LocalDateTime sessionStart = LocalDateTime.of(
                writeDTO.getVisitDate(), writeDTO.getStartTime());
        if (writeDTO.getBookingEndAt().isAfter(sessionStart)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "预约结束时间不能晚于场次开始时间");
        }

        long allocatedTotal = writeDTO.getTicketTypes().stream()
                .mapToLong(SessionTicketTypeConfigDTO::getAllocatedQuantity)
                .sum();
        if (allocatedTotal > writeDTO.getTotalCapacity()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "票种分配总量不能超过场次容量");
        }

        List<Long> ticketTypeIds = writeDTO.getTicketTypes().stream()
                .map(SessionTicketTypeConfigDTO::getTicketTypeId)
                .toList();
        Set<Long> uniqueIds = new HashSet<>(ticketTypeIds);
        if (uniqueIds.size() != ticketTypeIds.size()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "同一场次不能重复配置票种");
        }
        if (sessionMapper.countEnabledTicketTypes(venueId, ticketTypeIds) != ticketTypeIds.size()) {
            throw new BusinessException(ErrorCode.CONFLICT, "票种不存在、已停用或不属于当前景点");
        }
    }

    /**
     * 将请求参数转换为新建或更新场次使用的实体。
     */
    private AdmissionSession buildSession(SessionWriteDTO writeDTO, Long venueId) {
        AdmissionSession session = new AdmissionSession();
        session.setVenueId(venueId);
        session.setVisitDate(writeDTO.getVisitDate());
        session.setStartTime(writeDTO.getStartTime());
        session.setEndTime(writeDTO.getEndTime());
        session.setBookingStartAt(writeDTO.getBookingStartAt());
        session.setBookingEndAt(writeDTO.getBookingEndAt());
        session.setTotalCapacity(writeDTO.getTotalCapacity());
        session.setRemainingCapacity(writeDTO.getTotalCapacity());
        session.setStatus(AdmissionSessionStatus.DRAFT);
        return session;
    }

    /**
     * 为票种配置设置固定配额、初始余量和默认销售状态。
     */
    private List<SessionTicketType> buildTicketTypes(
            Long sessionId, SessionWriteDTO writeDTO) {
        return writeDTO.getTicketTypes().stream().map(config -> {
            SessionTicketType ticketType = new SessionTicketType();
            ticketType.setSessionId(sessionId);
            ticketType.setTicketTypeId(config.getTicketTypeId());
            ticketType.setSalePrice(config.getSalePrice());
            ticketType.setAllocatedQuantity(config.getAllocatedQuantity());
            ticketType.setRemainingQuantity(config.getAllocatedQuantity());
            ticketType.setStatus(SaleStatus.ON_SALE);
            return ticketType;
        }).toList();
    }

    /**
     * 查询场次票种静态快照。
     *
     * 查询顺序：
     * Caffeine -> Redis -> MySQL
     */
    @Override
    public SessionStaticSnapshotVO getSessionStaticSnapshot(Long sessionId) {

        SessionStaticSnapshotVO cachedSnapshot =
                sessionStaticLocalCache.get(
                        sessionId,

                        /*
                         * 只有Caffeine未命中时才执行这里。
                         * CacheClient内部继续完成Redis和MySQL查询。
                         */
                        id -> cacheClient.queryWithPassThrough(
                                SessionCacheConstant.SESSION_STATIC_PREFIX,
                                id,
                                SessionStaticSnapshotVO.class,
                                sessionMapper::findStaticSnapshot,
                                SessionCacheConstant.SESSION_STATIC_TTL,
                                TimeUnit.SECONDS
                        )
                );

        if (cachedSnapshot == null) {
            throw new BusinessException(
                    ErrorCode.NOT_FOUND,
                    "场次不存在或当前不可展示"
            );
        }

        /*
         * 返回深拷贝，避免调用方在补充动态库存时，
         * 直接修改Caffeine中保存的静态对象。
         */
        return copyStaticSnapshot(cachedSnapshot);
    }


    /**
     * 删除场次静态快照的二级缓存。
     */
    @Override
    public void evictSessionStaticSnapshot(Long sessionId) {

        /*
         * 先删除Redis，再删除Caffeine。
         *
         * 避免Caffeine刚被删除时，
         * 并发请求从旧Redis重新回填本地缓存。
         */
        cacheClient.delete(
                SessionCacheConstant.SESSION_STATIC_PREFIX + sessionId
        );

        sessionStaticLocalCache.invalidate(sessionId);
    }

    /**
     * 基础票种发生变化时，找出所有引用它的场次进行精确缓存失效。
     */
    @Override
    public List<Long> listSessionIdsByTicketTypeId(Long ticketTypeId) {
        return sessionMapper.listSessionIdsByTicketTypeId(ticketTypeId);
    }

    @Override
    @Transactional
    public int maintainLifecycle(LocalDateTime now) {
        int closed = sessionMapper.closeExpiredBookings(now);
        int ended = sessionMapper.endExpiredSessions(now);
        return closed + ended;
    }

    /**
     * 深拷贝静态快照。
     *
     * 后期开售后需要向响应中补充动态库存，
     * 因此不能直接把Caffeine中的原对象交给调用方修改。
     */
    private SessionStaticSnapshotVO copyStaticSnapshot(
            SessionStaticSnapshotVO source) {

        SessionStaticSnapshotVO target =
                new SessionStaticSnapshotVO();

        target.setSessionId(source.getSessionId());
        target.setVenueId(source.getVenueId());
        target.setVisitDate(source.getVisitDate());
        target.setStartTime(source.getStartTime());
        target.setEndTime(source.getEndTime());
        target.setBookingStartAt(source.getBookingStartAt());
        target.setBookingEndAt(source.getBookingEndAt());

        target.setTicketTypes(
                source.getTicketTypes()
                        .stream()
                        .map(this::copyStaticTicketType)
                        .toList()
        );

        return target;
    }

    private SessionTicketTypeStaticVO copyStaticTicketType(
            SessionTicketTypeStaticVO source) {

        SessionTicketTypeStaticVO target =
                new SessionTicketTypeStaticVO();

        target.setSessionTicketTypeId(
                source.getSessionTicketTypeId()
        );
        target.setTicketTypeId(source.getTicketTypeId());
        target.setTicketTypeName(source.getTicketTypeName());
        target.setDescription(source.getDescription());
        target.setAudienceRule(source.getAudienceRule());
        target.setSalePrice(source.getSalePrice());

        return target;
    }
}
