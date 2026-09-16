package com.qinghuan.pojo.entity;

import com.qinghuan.pojo.enums.VerificationResult;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class VerificationRecord {

    private Long id;
    private String requestNo;
    private Long ticketId;
    private Long verifierId;
    /** 核销人名称在核销时固定，运营记录查询不再关联 user_account。 */
    private String verifierNameSnapshot;
    private VerificationResult result;
    private String failureReason;
    private String deviceNo;
    private LocalDateTime verifiedAt;
}
