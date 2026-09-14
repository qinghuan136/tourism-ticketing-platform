package com.qinghuan.visitor;

import com.qinghuan.pojo.enums.VisitorStatus;
import lombok.Getter;
import lombok.Setter;

/** 下单校验使用的参观人资料，额外携带稳定的证件身份指纹。 */
@Getter
@Setter
public class VisitorForOrder {

    private Long id;
    private Long userId;
    private String name;
    private String idType;
    private String idNumber;
    private VisitorStatus status;
    private String fingerprint;
}
