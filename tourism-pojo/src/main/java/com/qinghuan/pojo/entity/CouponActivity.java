package com.qinghuan.pojo.entity;

import com.qinghuan.pojo.enums.CouponActivityStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 运营者发布的固定金额满减券活动。 */
@Getter
@Setter
public class CouponActivity extends BaseEntity {
    private Long venueId;
    /** 创建活动时写入的景点名称，用于公开展示和领券快照。 */
    private String venueNameSnapshot;
    private String name;
    private BigDecimal thresholdAmount;
    private BigDecimal discountAmount;
    private Integer totalStock;
    /** MySQL 最终库存；Redis 仅承担入口预扣。 */
    private Integer remainingStock;
    private LocalDateTime claimStartAt;
    private LocalDateTime claimEndAt;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private CouponActivityStatus status;
    /** 核心链路完成 Redis 预热后才能置为 true。 */
    private Boolean cacheReady;
    /** 最近一次成功完成整组 Redis 数据预热的时间。 */
    private LocalDateTime preheatedAt;
    /** 创建活动的运营者账号，和 venueId 一起保留审计信息。 */
    private Long createdBy;
}
