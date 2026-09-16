package com.qinghuan.visitor;

import com.qinghuan.pojo.dto.VisitorCreateDTO;
import com.qinghuan.pojo.dto.VisitorUpdateDTO;
import com.qinghuan.pojo.enums.VisitorStatus;
import com.qinghuan.pojo.vo.VisitorVO;

import java.util.List;

public interface VisitorService {

    /** 查询当前游客名下的参观人。 */
    List<VisitorVO> listVisitors(VisitorStatus status);

    /** 新建当前游客名下的参观人。 */
    Long createVisitor(VisitorCreateDTO createDTO);

    /** 获取当前游客名下的参观人详情。 */
    VisitorVO getVisitor(Long visitorId);

    /** 修改参观人的姓名和手机号。 */
    void updateVisitor(Long visitorId, VisitorUpdateDTO updateDTO);

    /** 启用或停用参观人。 */
    void updateVisitorStatus(Long visitorId, VisitorStatus status);

    /** 查询当前游客的有效参观人及证件指纹，供下单校验归属并生成快照。 */
    List<VisitorForOrder> listActiveVisitorsForOrder();
}
