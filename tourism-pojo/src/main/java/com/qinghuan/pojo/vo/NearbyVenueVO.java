package com.qinghuan.pojo.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** 游客端附近景点展示信息。 */
@Getter
@Setter
public class NearbyVenueVO {

    private Long id;
    private String name;
    private String address;
    private String description;
    private String coverUrl;
    /** 景点与查询中心之间的距离，单位为公里。 */
    private BigDecimal distanceKm;
}
