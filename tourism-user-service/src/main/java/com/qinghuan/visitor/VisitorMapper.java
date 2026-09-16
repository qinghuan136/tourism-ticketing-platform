package com.qinghuan.visitor;

import com.qinghuan.pojo.dto.VisitorUpdateDTO;
import com.qinghuan.pojo.entity.Visitor;
import com.qinghuan.pojo.enums.VisitorStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface VisitorMapper {

    /** 查询当前游客名下的参观人。 */
    List<Visitor> list(@Param("userId") Long userId,
                       @Param("status") VisitorStatus status);

    /** 按游客归属查询参观人详情。 */
    Visitor findById(@Param("id") Long id, @Param("userId") Long userId);

    /** 新建参观人并回填主键。 */
    int insert(Visitor visitor);

    /** 保存参观人的稳定证件身份指纹。 */
    int insertIdentity(@Param("visitorId") Long visitorId,
                       @Param("fingerprint") String fingerprint);

    /** 查询下单所需的有效参观人及其证件指纹。 */
    List<VisitorForOrder> listActiveForOrder(@Param("userId") Long userId);

    /** 修改当前游客名下参观人的姓名和手机号。 */
    int update(@Param("id") Long id,
               @Param("userId") Long userId,
               @Param("updateDTO") VisitorUpdateDTO updateDTO);

    /** 修改当前游客名下参观人的启用状态。 */
    int updateStatus(@Param("id") Long id,
                     @Param("userId") Long userId,
                     @Param("status") VisitorStatus status);
}
