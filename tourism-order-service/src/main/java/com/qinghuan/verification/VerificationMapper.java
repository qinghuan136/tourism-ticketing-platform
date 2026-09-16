package com.qinghuan.verification;

import com.qinghuan.pojo.dto.VerificationPageQueryDTO;
import com.qinghuan.pojo.entity.VerificationRecord;
import com.qinghuan.pojo.vo.VerificationRecordVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface VerificationMapper {

    /** 按幂等请求号查询已有核销结果。 */
    VerificationRecord findByRequestNo(String requestNo);

    /** 唯一键冲突后使用当前读取得已提交的幂等结果。 */
    VerificationRecord findByRequestNoForUpdate(String requestNo);

    /** 保存成功或失败的核销记录。 */
    int insertRecord(VerificationRecord record);

    /** 条件核销失败时，将当前请求记录修正为失败结果。 */
    int updateResult(VerificationRecord record);

    /** 按当前景点范围查询核销记录，供 PageHelper 分页。 */
    List<VerificationRecordVO> listRecords(
            @Param("venueId") Long venueId,
            @Param("query") VerificationPageQueryDTO queryDTO);
}
