package com.qinghuan.pojo.vo;

import com.qinghuan.pojo.enums.VenueStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** 运营端景点资料，封面返回可访问地址而不是 OSS objectKey。 */
@Getter
@Setter
public class VenueVO {
    private Long id;
    private String name;
    private String address;
    private String description;
    private String coverUrl;
    private VenueStatus status;
    private BigDecimal longitude;
    private BigDecimal latitude;
}
