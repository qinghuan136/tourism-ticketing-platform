package com.qinghuan.tickettype;

import com.qinghuan.pojo.dto.TicketTypePageQueryDTO;
import com.qinghuan.pojo.dto.TicketTypeUpdateDTO;
import com.qinghuan.pojo.entity.TicketType;
import com.qinghuan.pojo.vo.PageResult;

import java.util.List;

public interface TicketTypeService {
    // 分页查询
    public PageResult<TicketType> pageQueryTicketType(TicketTypePageQueryDTO ticketTypePageQueryDTO);
    // 获取票种详情
    public TicketType getTicketTypeById(Long id);
    // 新建票种
    public Integer createTicketType(TicketTypeUpdateDTO ticketType);
    // 修改票种
    void updateTicketType(Long id, TicketTypeUpdateDTO updateDTO);
    // 批量删除票种
    public void deleteBatch(List<Long> ids);
}
