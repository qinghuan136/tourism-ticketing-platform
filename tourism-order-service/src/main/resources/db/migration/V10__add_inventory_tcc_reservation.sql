CREATE TABLE inventory_reservation_order (
    order_id BIGINT UNSIGNED NOT NULL COMMENT '预生成的订单ID',
    session_id BIGINT UNSIGNED NULL COMMENT '场次ID；Cancel 先到时允许为空',
    status VARCHAR(20) NOT NULL COMMENT 'RESERVED、CONFIRMED 或 CANCELED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (order_id),
    KEY idx_inventory_reservation_order_status (status, updated_at),
    CONSTRAINT chk_inventory_reservation_order_status
        CHECK (status IN ('RESERVED', 'CONFIRMED', 'CANCELED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '场次库存TCC预留主记录';

CREATE TABLE inventory_reservation (
    order_id BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
    session_ticket_type_id BIGINT UNSIGNED NOT NULL COMMENT '场次票种ID',
    quantity INT UNSIGNED NOT NULL COMMENT '预留数量',
    status VARCHAR(20) NOT NULL COMMENT 'RESERVED、CONFIRMED 或 CANCELED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (order_id, session_ticket_type_id),
    CONSTRAINT fk_inventory_reservation_order
        FOREIGN KEY (order_id) REFERENCES inventory_reservation_order(order_id)
            ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_inventory_reservation_ticket_type
        FOREIGN KEY (session_ticket_type_id) REFERENCES session_ticket_type(id)
            ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_inventory_reservation_quantity CHECK (quantity > 0),
    CONSTRAINT chk_inventory_reservation_status
        CHECK (status IN ('RESERVED', 'CONFIRMED', 'CANCELED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '场次库存TCC预留明细';

CREATE TABLE booking_inventory_tcc_operation (
    order_id BIGINT UNSIGNED NOT NULL COMMENT '预生成的订单ID',
    session_id BIGINT UNSIGNED NOT NULL COMMENT '场次ID',
    status VARCHAR(20) NOT NULL COMMENT '订单侧TCC协调状态',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (order_id),
    KEY idx_booking_inventory_tcc_status (status, updated_at),
    CONSTRAINT chk_booking_inventory_tcc_status
        CHECK (status IN ('TRYING', 'CONFIRMING', 'CANCELING', 'CONFIRMED', 'CANCELED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单创建库存TCC协调记录';
