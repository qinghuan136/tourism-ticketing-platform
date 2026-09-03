package com.qinghuan.pojo.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** 游客端景点列表和详情展示信息。 */
@Getter
@Setter
public class CatalogVenueVO {

    private Long id;
    private String name;
    private String address;
    private String description;
    private String coverUrl;

    /** 当前可售场次票种的最低售价；暂不可预约时为 null。 */
    private BigDecimal minimumPrice;
}
