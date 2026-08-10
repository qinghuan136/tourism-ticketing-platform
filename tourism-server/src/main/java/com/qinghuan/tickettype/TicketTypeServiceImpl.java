package com.qinghuan.tickettype;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qinghuan.annotation.RefreshCreateTimeOrUpdateTime;
import com.qinghuan.auth.context.UserContext;
import com.qinghuan.common.exception.BusinessException;
import com.qinghuan.common.exception.ErrorCode;
import com.qinghuan.pojo.dto.TicketTypePageQueryDTO;
import com.qinghuan.pojo.dto.TicketTypeUpdateDTO;
import com.qinghuan.pojo.entity.TicketType;
import com.qinghuan.pojo.enums.OperationType;
import com.qinghuan.pojo.vo.PageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

@Slf4j
@Service
public class TicketTypeServiceImpl implements TicketTypeService {
    private final TicketTypeMapper ticketTypeMapper;

    public TicketTypeServiceImpl(TicketTypeMapper ticketTypeMapper) {
        this.ticketTypeMapper = ticketTypeMapper;
    }


    /*
     * 分页查询票种
     */
    @Override
    public PageResult<TicketType> pageQueryTicketType(TicketTypePageQueryDTO ticketTypePageQueryDTO) {
        //pageHelper
        PageHelper.startPage(ticketTypePageQueryDTO.getPage(), ticketTypePageQueryDTO.getSize());

        //查询当前景点的数据
        List<TicketType> ticketTypes = ticketTypeMapper.list(
                UserContext.getRequired().venueId(), ticketTypePageQueryDTO.getKeyword());
        Page<TicketType> page = (Page<TicketType>) ticketTypes;

        //打包返回
        return new PageResult<TicketType>(page.getResult(), page.getTotal(), page.getPageNum(), page.getPageSize());
    }

    /**
     * 根据id查询票种
     */
    @Override
    public TicketType getTicketTypeById(Long id) {
        // 根据id和venueId查询票种
        TicketType ticketType = ticketTypeMapper.getTicketTypeById(id, UserContext.getRequired().venueId());
        if (ticketType == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "票种不存在！");
        }
        return ticketType;
    }

    /*
     * 创建票种
     */
    @Override
    @RefreshCreateTimeOrUpdateTime(OperationType.INSERT)
    public Integer createTicketType(TicketTypeUpdateDTO createDTO) {
        TicketType ticketType = new TicketType();
        ticketType.setVenueId(UserContext.getRequired().venueId());
        ticketType.setName(createDTO.getName().trim());
        ticketType.setDescription(createDTO.getDescription());
        ticketType.setAudienceRule(createDTO.getAudienceRule());
        ticketType.setBasePrice(createDTO.getBasePrice());
        ticketType.setStatus(createDTO.getStatus());
        ticketType.setCreatedAt(LocalDateTime.now());
        ticketType.setUpdatedAt(LocalDateTime.now());
        try {
            return ticketTypeMapper.insert(ticketType);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.CONFLICT, "票种已存在！");
        }

    }

    @Override
    public void updateTicketType(Long id, TicketTypeUpdateDTO updateDTO) {
        try {
            int updatedRows = ticketTypeMapper.updateTicketType(
                    id, UserContext.getRequired().venueId(), updateDTO);
            if (updatedRows == 0) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "票种不存在或不属于当前景点");
            }
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.CONFLICT, "当前景点已存在同名票种");
        }
    }

    @Transactional
    @Override
    public void deleteBatch(List<Long> ids) {
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "票种ID不能重复");
        }

        // 检查删除票种的venueId
        List<TicketType> ticketTypes = ticketTypeMapper.getTicketTypesByIds(ids);
        if (ticketTypes.size() != ids.size()
                || ticketTypes.stream().anyMatch(ticketType ->
                !ticketType.getVenueId().equals(UserContext.getRequired().venueId()))) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "一个或多个票种不存在");
        }

        // 检查删除票种的关联场次
        List<Long> sessionIds = ticketTypeMapper.getSessionIdsByTicketTypeIds(ids);
        if (sessionIds != null && !sessionIds.isEmpty()) {
            throw new BusinessException(ErrorCode.CONFLICT, "票种有关联场次，请先删除关联场次！");
        }

        // 批量删除票种
        ticketTypeMapper.deleteBatch(ids);
    }

}
