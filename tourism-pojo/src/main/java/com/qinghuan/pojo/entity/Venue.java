package com.qinghuan.pojo.entity;

import com.qinghuan.pojo.enums.VenueStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class Venue extends BaseEntity {

    private String name;
    private String address;
    private String description;
    private String coverObjectKey;
    private VenueStatus status;
    /** 景点经度，Redis GEO 写入时必须放在纬度前面。 */
    private BigDecimal longitude;
    /** 景点纬度。 */
    private BigDecimal latitude;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
