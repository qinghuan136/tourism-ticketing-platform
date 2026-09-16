-- 优惠券活动。发布只改变数据库状态，Redis 预热由独立核心链路完成。
CREATE TABLE `coupon_activity` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '优惠券活动ID',
    `venue_id` BIGINT UNSIGNED NOT NULL COMMENT '所属景点ID',
    `name` VARCHAR(100) NOT NULL COMMENT '活动及优惠券名称',
    `threshold_amount` DECIMAL(10, 2) NOT NULL COMMENT '订单使用门槛',
    `discount_amount` DECIMAL(10, 2) NOT NULL COMMENT '固定优惠金额',
    `total_stock` INT UNSIGNED NOT NULL COMMENT '总发行量',
    `remaining_stock` INT UNSIGNED NOT NULL COMMENT '数据库最终剩余量',
    `claim_start_at` DATETIME NOT NULL COMMENT '开始领取时间',
    `claim_end_at` DATETIME NOT NULL COMMENT '结束领取时间',
    `valid_from` DATETIME NOT NULL COMMENT '优惠券生效时间',
    `valid_until` DATETIME NOT NULL COMMENT '优惠券失效时间',
    `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '活动状态',
    `cache_ready` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Redis领取数据是否已完成预热',
    `preheated_at` DATETIME NULL COMMENT '最近一次成功预热时间',
    `created_by` BIGINT UNSIGNED NOT NULL COMMENT '创建运营者ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',

    PRIMARY KEY (`id`),
    KEY `idx_coupon_activity_venue_status_time`
        (`venue_id`, `status`, `claim_start_at`, `claim_end_at`),
    KEY `idx_coupon_activity_preheat`
        (`status`, `cache_ready`, `claim_start_at`),
    CONSTRAINT `fk_coupon_activity_venue`
        FOREIGN KEY (`venue_id`) REFERENCES `venue` (`id`)
            ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_coupon_activity_creator`
        FOREIGN KEY (`created_by`) REFERENCES `user_account` (`id`)
            ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `chk_coupon_activity_amount`
        CHECK (`threshold_amount` >= `discount_amount` AND `discount_amount` > 0),
    CONSTRAINT `chk_coupon_activity_stock`
        CHECK (`total_stock` > 0 AND `remaining_stock` <= `total_stock`),
    CONSTRAINT `chk_coupon_activity_claim_time`
        CHECK (`claim_start_at` < `claim_end_at`),
    CONSTRAINT `chk_coupon_activity_valid_time`
        CHECK (`valid_from` < `valid_until` AND `valid_until` > `claim_start_at`),
    CONSTRAINT `chk_coupon_activity_status`
        CHECK (`status` IN ('DRAFT', 'PUBLISHED', 'ENDED', 'CANCELLED')),
    CONSTRAINT `chk_coupon_activity_cache_ready`
        CHECK (`cache_ready` IN (0, 1))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '优惠券秒杀活动';

-- 游客领取后的券使用活动规则快照，活动取消不会影响已经发放的券。
CREATE TABLE `user_coupon` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '游客优惠券ID',
    `activity_id` BIGINT UNSIGNED NOT NULL COMMENT '来源活动ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '领取游客账号ID',
    `venue_id` BIGINT UNSIGNED NOT NULL COMMENT '适用景点ID',
    `coupon_name` VARCHAR(100) NOT NULL COMMENT '优惠券名称快照',
    `threshold_amount` DECIMAL(10, 2) NOT NULL COMMENT '使用门槛快照',
    `discount_amount` DECIMAL(10, 2) NOT NULL COMMENT '优惠金额快照',
    `valid_from` DATETIME NOT NULL COMMENT '生效时间快照',
    `valid_until` DATETIME NOT NULL COMMENT '失效时间快照',
    `status` VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' COMMENT '优惠券状态',
    `acquired_at` DATETIME NOT NULL COMMENT '领取时间',
    `used_at` DATETIME NULL COMMENT '最近一次正式使用时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_coupon_activity_user` (`activity_id`, `user_id`),
    KEY `idx_user_coupon_user_status_valid` (`user_id`, `status`, `valid_until`),
    KEY `idx_user_coupon_venue_status` (`venue_id`, `status`),
    CONSTRAINT `fk_user_coupon_activity`
        FOREIGN KEY (`activity_id`) REFERENCES `coupon_activity` (`id`)
            ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_user_coupon_user`
        FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`)
            ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_user_coupon_venue`
        FOREIGN KEY (`venue_id`) REFERENCES `venue` (`id`)
            ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `chk_user_coupon_status`
        CHECK (`status` IN ('AVAILABLE', 'LOCKED', 'USED', 'EXPIRED')),
    CONSTRAINT `chk_user_coupon_amount`
        CHECK (`threshold_amount` >= `discount_amount` AND `discount_amount` > 0),
    CONSTRAINT `chk_user_coupon_valid_time`
        CHECK (`valid_from` < `valid_until`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '游客领取的优惠券';

-- 异步抢券请求的最终状态；写入和消费逻辑留给 Kafka 核心链路实现。
CREATE TABLE `coupon_claim_request` (
    `request_id` VARCHAR(64) NOT NULL COMMENT '异步请求号',
    `activity_id` BIGINT UNSIGNED NOT NULL COMMENT '目标活动ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '请求游客ID',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '处理状态',
    `user_coupon_id` BIGINT UNSIGNED NULL COMMENT '成功生成的优惠券ID',
    `failure_reason` VARCHAR(50) NULL COMMENT '业务失败原因',
    `requested_at` DATETIME NOT NULL COMMENT '请求时间',
    `processed_at` DATETIME NULL COMMENT '处理完成时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',

    PRIMARY KEY (`request_id`),
    UNIQUE KEY `uk_coupon_claim_activity_user` (`activity_id`, `user_id`),
    KEY `idx_coupon_claim_status_created` (`status`, `created_at`),
    CONSTRAINT `fk_coupon_claim_activity`
        FOREIGN KEY (`activity_id`) REFERENCES `coupon_activity` (`id`)
            ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_coupon_claim_user`
        FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`)
            ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_coupon_claim_user_coupon`
        FOREIGN KEY (`user_coupon_id`) REFERENCES `user_coupon` (`id`)
            ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `chk_coupon_claim_status`
        CHECK (`status` IN ('PENDING', 'SUCCESS', 'FAILED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '优惠券异步领取请求';

-- 历史订单先把优惠前金额回填为原 total_amount，再收紧为非空字段。
ALTER TABLE `booking_order`
    ADD COLUMN `user_coupon_id` BIGINT UNSIGNED NULL COMMENT '使用的游客优惠券ID'
        AFTER `quantity`,
    ADD COLUMN `original_amount` DECIMAL(10, 2) NULL COMMENT '优惠前金额'
        AFTER `user_coupon_id`,
    ADD COLUMN `discount_amount` DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额'
        AFTER `original_amount`;

UPDATE `booking_order`
SET `original_amount` = `total_amount`
WHERE `original_amount` IS NULL;

ALTER TABLE `booking_order`
    MODIFY COLUMN `original_amount` DECIMAL(10, 2) NOT NULL COMMENT '优惠前金额',
    ADD KEY `idx_booking_order_user_coupon_id` (`user_coupon_id`),
    ADD CONSTRAINT `fk_booking_order_user_coupon`
        FOREIGN KEY (`user_coupon_id`) REFERENCES `user_coupon` (`id`)
            ON DELETE RESTRICT ON UPDATE CASCADE,
    ADD CONSTRAINT `chk_booking_order_original_amount`
        CHECK (`original_amount` >= 0),
    ADD CONSTRAINT `chk_booking_order_discount_amount`
        CHECK (`discount_amount` >= 0 AND `discount_amount` <= `original_amount`);
