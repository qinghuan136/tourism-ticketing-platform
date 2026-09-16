-- 优惠券由 coupon-service 管理；订单号作为 Try/Confirm/Cancel 的幂等业务键。
ALTER TABLE user_coupon
    ADD COLUMN locked_order_id BIGINT UNSIGNED NULL COMMENT '当前锁券所属订单ID' AFTER used_at,
    ADD COLUMN used_order_id BIGINT UNSIGNED NULL COMMENT '正式核销所属订单ID' AFTER locked_order_id,
    ADD KEY idx_user_coupon_locked_order (locked_order_id),
    ADD KEY idx_user_coupon_used_order (used_order_id);

CREATE TABLE coupon_order_reservation (
    order_id BIGINT UNSIGNED NOT NULL COMMENT '预生成订单ID',
    coupon_id BIGINT UNSIGNED NOT NULL COMMENT '游客优惠券ID',
    status VARCHAR(20) NOT NULL COMMENT 'RESERVED、CONFIRMED 或 CANCELED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (order_id),
    KEY idx_coupon_order_reservation_status (status, updated_at),
    CONSTRAINT chk_coupon_order_reservation_status
        CHECK (status IN ('RESERVED', 'CONFIRMED', 'CANCELED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '优惠券订单TCC预留记录';

CREATE TABLE booking_coupon_tcc_operation (
    order_id BIGINT UNSIGNED NOT NULL COMMENT '预生成订单ID',
    coupon_id BIGINT UNSIGNED NOT NULL COMMENT '游客优惠券ID',
    status VARCHAR(20) NOT NULL COMMENT '订单侧优惠券TCC协调状态',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (order_id),
    KEY idx_booking_coupon_tcc_status (status, updated_at),
    CONSTRAINT chk_booking_coupon_tcc_status
        CHECK (status IN ('TRYING', 'TRIED', 'CONFIRMING', 'CANCELING', 'CONFIRMED', 'CANCELED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单创建优惠券TCC协调记录';
