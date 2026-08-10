package com.qinghuan.tickettype;

import com.qinghuan.pojo.dto.TicketTypeUpdateDTO;
import com.qinghuan.pojo.entity.TicketType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TicketTypeMapper {
    // 列出景点票种
    public List<TicketType> list(@Param("venueId") Long venueId,
                                 @Param("keyword") String keyword);
    // 获取票种详情
    public TicketType getTicketTypeById(Long id, Long venueId);
    // 新建票种
    public Integer insert(TicketType ticketType);
    // 修改当前景点的票种
    int updateTicketType(@Param("id") Long id,
                         @Param("venueId") Long venueId,
                         @Param("updateDTO") TicketTypeUpdateDTO updateDTO);
    // 根据ids批量查询票种
    public List<TicketType> getTicketTypesByIds(List<Long> ids);
    // 根据ids查询关联场次的sessionIds
    public List<Long> getSessionIdsByTicketTypeIds(List<Long> ids);
    // 批量删除票种
    public void deleteBatch(List<Long> ids);

}
