package com.qinghuan.verification;

import com.qinghuan.pojo.dto.VerificationPageQueryDTO;
import com.qinghuan.pojo.dto.VerificationRequestDTO;
import com.qinghuan.pojo.vo.PageResult;
import com.qinghuan.pojo.vo.VerificationRecordVO;
import com.qinghuan.pojo.vo.VerificationResultVO;

public interface VerificationService {

    /** 处理一次幂等的票券核销请求。 */
    VerificationResultVO verify(VerificationRequestDTO requestDTO);

    /** 分页查询当前景点核销记录。 */
    PageResult<VerificationRecordVO> pageRecords(VerificationPageQueryDTO queryDTO);
}
