-- 文旅场馆预约购票系统：MySQL 建表语句
-- 字符集：utf8mb4；存储引擎：InnoDB
-- 请先切换到目标数据库后再执行本文件。

SET NAMES utf8mb4;

-- =========================================================
-- 1. 景点表
-- =========================================================
CREATE TABLE `venue` (
                         `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '景点ID',
                         `name` VARCHAR(100) NOT NULL COMMENT '景点名称',
                         `address` VARCHAR(255) NOT NULL COMMENT '景点地址',
                         `description` TEXT NULL COMMENT '景点简介',
                         `cover_url` VARCHAR(500) NULL COMMENT '封面图片地址',
                         `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '景点状态',
                         `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                         `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                             ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',

                         PRIMARY KEY (`id`),
                         UNIQUE KEY `uk_venue_name_address` (`name`, `address`),
                         CONSTRAINT `chk_venue_status`
                             CHECK (`status` IN ('ENABLED', 'DISABLED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '景点或文旅场馆';

-- =========================================================
-- 2. 用户账号表
-- =========================================================
CREATE TABLE `user_account` (
                                `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
                                `login_name` VARCHAR(50) NOT NULL COMMENT '登录名',
                                `password_hash` VARCHAR(255) NOT NULL COMMENT '加密后的密码',
                                `display_name` VARCHAR(50) NOT NULL COMMENT '用户显示名称',
                                `phone` VARCHAR(20) NULL COMMENT '手机号',
                                `role_code` VARCHAR(20) NOT NULL COMMENT '用户角色',
                                `venue_id` BIGINT UNSIGNED NULL COMMENT '所属景点ID',
                                `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '账号状态',
                                `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                                    ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',

                                PRIMARY KEY (`id`),
                                UNIQUE KEY `uk_user_account_login_name` (`login_name`),
                                UNIQUE KEY `uk_user_account_phone` (`phone`),
                                KEY `idx_user_account_venue_id` (`venue_id`),
                                CONSTRAINT `fk_user_account_venue`
                                    FOREIGN KEY (`venue_id`) REFERENCES `venue` (`id`)
                                        ON DELETE RESTRICT ON UPDATE CASCADE,
                                CONSTRAINT `chk_user_account_role_code`
                                    CHECK (`role_code` IN ('TOURIST', 'OPERATOR', 'STAFF', 'ADMIN')),
                                CONSTRAINT `chk_user_account_status`
                                    CHECK (`status` IN ('ACTIVE', 'DISABLED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '游客、运营者、工作人员和管理员账号';

-- =========================================================
-- 3. 参观人表
-- =========================================================
CREATE TABLE `visitor` (
                           `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '参观人ID',
                           `user_id` BIGINT UNSIGNED NOT NULL COMMENT '所属游客账号ID',
                           `name` VARCHAR(50) NOT NULL COMMENT '真实姓名',
                           `id_type` VARCHAR(20) NOT NULL COMMENT '证件类型',
                           `id_number` VARCHAR(64) NOT NULL COMMENT '证件号码',
                           `phone` VARCHAR(20) NULL COMMENT '联系电话',
                           `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '参观人状态',
                           `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                           `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                               ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',

                           PRIMARY KEY (`id`),
                           UNIQUE KEY `uk_visitor_user_document` (`user_id`, `id_type`, `id_number`),
                           CONSTRAINT `fk_visitor_user_account`
                               FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`)
                                   ON DELETE RESTRICT ON UPDATE CASCADE,
                           CONSTRAINT `chk_visitor_status`
                               CHECK (`status` IN ('ACTIVE', 'DISABLED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '游客维护的实际参观人员';

-- =========================================================
-- 4. 预约场次表
-- =========================================================
CREATE TABLE `admission_session` (
                                     `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '场次ID',
                                     `venue_id` BIGINT UNSIGNED NOT NULL COMMENT '所属景点ID',
                                     `visit_date` DATE NOT NULL COMMENT '参观日期',
                                     `start_time` TIME NOT NULL COMMENT '入场时段开始时间',
                                     `end_time` TIME NOT NULL COMMENT '入场时段结束时间',
                                     `booking_start_at` DATETIME NOT NULL COMMENT '开始预约时间',
                                     `booking_end_at` DATETIME NOT NULL COMMENT '停止预约时间',
                                     `total_capacity` INT UNSIGNED NOT NULL COMMENT '场次总容量',
                                     `remaining_capacity` INT UNSIGNED NOT NULL COMMENT '场次剩余容量',
                                     `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '场次状态',
                                     `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                     `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                                         ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',

                                     PRIMARY KEY (`id`),
                                     UNIQUE KEY `uk_session_venue_date_time`
                                         (`venue_id`, `visit_date`, `start_time`, `end_time`),
                                     KEY `idx_session_venue_date_status` (`venue_id`, `visit_date`, `status`),
                                     CONSTRAINT `fk_admission_session_venue`
                                         FOREIGN KEY (`venue_id`) REFERENCES `venue` (`id`)
                                             ON DELETE RESTRICT ON UPDATE CASCADE,
                                     CONSTRAINT `chk_admission_session_time_range`
                                         CHECK (`start_time` < `end_time`),
                                     CONSTRAINT `chk_admission_session_booking_range`
                                         CHECK (`booking_start_at` < `booking_end_at`),
                                     CONSTRAINT `chk_admission_session_total_capacity`
                                         CHECK (`total_capacity` > 0),
                                     CONSTRAINT `chk_admission_session_remaining_capacity`
                                         CHECK (`remaining_capacity` <= `total_capacity`),
                                     CONSTRAINT `chk_admission_session_status`
                                         CHECK (`status` IN ('DRAFT', 'OPEN', 'CLOSED', 'ENDED', 'CANCELLED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '景点预约入场场次';

-- =========================================================
-- 5. 票种表
-- =========================================================
CREATE TABLE `ticket_type` (
                               `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '票种ID',
                               `venue_id` BIGINT UNSIGNED NOT NULL COMMENT '所属景点ID',
                               `name` VARCHAR(100) NOT NULL COMMENT '票种名称',
                               `description` VARCHAR(500) NULL COMMENT '票种说明',
                               `audience_rule` VARCHAR(500) NULL COMMENT '适用人群规则说明',
                               `base_price` DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '基础价格',
                               `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '票种状态',
                               `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                                   ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',

                               PRIMARY KEY (`id`),
                               UNIQUE KEY `uk_ticket_type_venue_name` (`venue_id`, `name`),
                               CONSTRAINT `fk_ticket_type_venue`
                                   FOREIGN KEY (`venue_id`) REFERENCES `venue` (`id`)
                                       ON DELETE RESTRICT ON UPDATE CASCADE,
                               CONSTRAINT `chk_ticket_type_base_price`
                                   CHECK (`base_price` >= 0),
                               CONSTRAINT `chk_ticket_type_status`
                                   CHECK (`status` IN ('ENABLED', 'DISABLED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '景点票种定义';

-- =========================================================
-- 6. 场次票种关联表
-- =========================================================
CREATE TABLE `session_ticket_type` (
                                       `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '场次票种配置ID',
                                       `session_id` BIGINT UNSIGNED NOT NULL COMMENT '场次ID',
                                       `ticket_type_id` BIGINT UNSIGNED NOT NULL COMMENT '票种ID',
                                       `sale_price` DECIMAL(10, 2) NOT NULL COMMENT '该场次实际售价',
                                       `status` VARCHAR(20) NOT NULL DEFAULT 'ON_SALE' COMMENT '销售状态',
                                       `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                       `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                                           ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',

                                       PRIMARY KEY (`id`),
                                       UNIQUE KEY `uk_session_ticket_type` (`session_id`, `ticket_type_id`),
                                       KEY `idx_session_ticket_type_ticket_type_id` (`ticket_type_id`),
                                       CONSTRAINT `fk_session_ticket_type_session`
                                           FOREIGN KEY (`session_id`) REFERENCES `admission_session` (`id`)
                                               ON DELETE RESTRICT ON UPDATE CASCADE,
                                       CONSTRAINT `fk_session_ticket_type_ticket_type`
                                           FOREIGN KEY (`ticket_type_id`) REFERENCES `ticket_type` (`id`)
                                               ON DELETE RESTRICT ON UPDATE CASCADE,
                                       CONSTRAINT `chk_session_ticket_type_sale_price`
                                           CHECK (`sale_price` >= 0),
                                       CONSTRAINT `chk_session_ticket_type_status`
                                           CHECK (`status` IN ('ON_SALE', 'OFF_SALE'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '场次可售票种及实际售价';

-- =========================================================
-- 7. 订单表
-- =========================================================
CREATE TABLE `booking_order` (
                                 `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '订单ID',
                                 `order_no` VARCHAR(32) NOT NULL COMMENT '业务订单号',
                                 `user_id` BIGINT UNSIGNED NOT NULL COMMENT '下单游客ID',
                                 `session_id` BIGINT UNSIGNED NOT NULL COMMENT '预约场次ID',
                                 `quantity` INT UNSIGNED NOT NULL COMMENT '订单票券数量',
                                 `total_amount` DECIMAL(10, 2) NOT NULL COMMENT '订单总金额',
                                 `status` VARCHAR(30) NOT NULL COMMENT '订单状态',
                                 `payment_no` VARCHAR(64) NULL COMMENT '模拟支付流水号',
                                 `expire_at` DATETIME NULL COMMENT '支付截止时间',
                                 `paid_at` DATETIME NULL COMMENT '支付成功时间',
                                 `cancelled_at` DATETIME NULL COMMENT '用户取消时间',
                                 `closed_at` DATETIME NULL COMMENT '超时关闭时间',
                                 `completed_at` DATETIME NULL COMMENT '订单完成时间',
                                 `refund_at` DATETIME NULL COMMENT '退款时间',
                                 `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                                     ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',

                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `uk_booking_order_order_no` (`order_no`),
                                 UNIQUE KEY `uk_booking_order_payment_no` (`payment_no`),
                                 KEY `idx_booking_order_user_created_at` (`user_id`, `created_at`),
                                 KEY `idx_booking_order_session_status` (`session_id`, `status`),
                                 CONSTRAINT `fk_booking_order_user_account`
                                     FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`)
                                         ON DELETE RESTRICT ON UPDATE CASCADE,
                                 CONSTRAINT `fk_booking_order_session`
                                     FOREIGN KEY (`session_id`) REFERENCES `admission_session` (`id`)
                                         ON DELETE RESTRICT ON UPDATE CASCADE,
                                 CONSTRAINT `chk_booking_order_quantity`
                                     CHECK (`quantity` > 0),
                                 CONSTRAINT `chk_booking_order_total_amount`
                                     CHECK (`total_amount` >= 0),
                                 CONSTRAINT `chk_booking_order_status`
                                     CHECK (`status` IN (
                                                         'PENDING_PAYMENT', 'PAID', 'CANCELLED',
                                                         'CLOSED', 'COMPLETED', 'REFUNDED'
                                         ))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '预约或购票订单';

-- =========================================================
-- 8. 订单明细表
-- =========================================================
CREATE TABLE `booking_order_item` (
                                      `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '订单明细ID',
                                      `order_id` BIGINT UNSIGNED NOT NULL COMMENT '所属订单ID',
                                      `visitor_id` BIGINT UNSIGNED NOT NULL COMMENT '参观人ID',
                                      `session_ticket_type_id` BIGINT UNSIGNED NOT NULL COMMENT '所选场次票种配置ID',
                                      `visitor_name` VARCHAR(50) NOT NULL COMMENT '下单时参观人姓名快照',
                                      `visitor_id_type` VARCHAR(20) NOT NULL COMMENT '下单时证件类型快照',
                                      `visitor_id_number` VARCHAR(64) NOT NULL COMMENT '下单时证件号快照',
                                      `ticket_type_name` VARCHAR(100) NOT NULL COMMENT '下单时票种名称快照',
                                      `unit_price` DECIMAL(10, 2) NOT NULL COMMENT '下单时单价快照',
                                      `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

                                      PRIMARY KEY (`id`),
                                      UNIQUE KEY `uk_booking_order_item_order_visitor` (`order_id`, `visitor_id`),
                                      KEY `idx_booking_order_item_visitor_id` (`visitor_id`),
                                      KEY `idx_booking_order_item_session_ticket_type_id` (`session_ticket_type_id`),
                                      CONSTRAINT `fk_booking_order_item_order`
                                          FOREIGN KEY (`order_id`) REFERENCES `booking_order` (`id`)
                                              ON DELETE RESTRICT ON UPDATE CASCADE,
                                      CONSTRAINT `fk_booking_order_item_visitor`
                                          FOREIGN KEY (`visitor_id`) REFERENCES `visitor` (`id`)
                                              ON DELETE RESTRICT ON UPDATE CASCADE,
                                      CONSTRAINT `fk_booking_order_item_session_ticket_type`
                                          FOREIGN KEY (`session_ticket_type_id`) REFERENCES `session_ticket_type` (`id`)
                                              ON DELETE RESTRICT ON UPDATE CASCADE,
                                      CONSTRAINT `chk_booking_order_item_unit_price`
                                          CHECK (`unit_price` >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '订单参观人、票种和价格快照';

-- =========================================================
-- 9. 电子票券表
-- =========================================================
CREATE TABLE `ticket` (
                          `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '票券ID',
                          `ticket_code` VARCHAR(64) NOT NULL COMMENT '唯一票码',
                          `order_item_id` BIGINT UNSIGNED NOT NULL COMMENT '对应订单明细ID',
                          `status` VARCHAR(20) NOT NULL DEFAULT 'VALID' COMMENT '票券状态',
                          `valid_from` DATETIME NOT NULL COMMENT '开始有效时间',
                          `valid_until` DATETIME NOT NULL COMMENT '失效时间',
                          `verified_at` DATETIME NULL COMMENT '成功核销时间',
                          `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                          `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                              ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',

                          PRIMARY KEY (`id`),
                          UNIQUE KEY `uk_ticket_ticket_code` (`ticket_code`),
                          UNIQUE KEY `uk_ticket_order_item_id` (`order_item_id`),
                          CONSTRAINT `fk_ticket_order_item`
                              FOREIGN KEY (`order_item_id`) REFERENCES `booking_order_item` (`id`)
                                  ON DELETE RESTRICT ON UPDATE CASCADE,
                          CONSTRAINT `chk_ticket_status`
                              CHECK (`status` IN ('VALID', 'USED', 'VOID', 'EXPIRED')),
                          CONSTRAINT `chk_ticket_validity_range`
                              CHECK (`valid_from` < `valid_until`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '支付成功后生成的独立入场凭证';

-- =========================================================
-- 10. 核销记录表
-- =========================================================
CREATE TABLE `verification_record` (
                                       `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '核销记录ID',
                                       `request_no` VARCHAR(64) NOT NULL COMMENT '核销请求号',
                                       `ticket_id` BIGINT UNSIGNED NOT NULL COMMENT '被核销票券ID',
                                       `verifier_id` BIGINT UNSIGNED NOT NULL COMMENT '核销工作人员ID',
                                       `result` VARCHAR(20) NOT NULL COMMENT '核销结果',
                                       `failure_reason` VARCHAR(255) NULL COMMENT '核销失败原因',
                                       `device_no` VARCHAR(64) NULL COMMENT '模拟核销设备编号',
                                       `verified_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '核销请求时间',

                                       PRIMARY KEY (`id`),
                                       UNIQUE KEY `uk_verification_record_request_no` (`request_no`),
                                       KEY `idx_verification_record_ticket_id` (`ticket_id`),
                                       KEY `idx_verification_record_verifier_id` (`verifier_id`),
                                       CONSTRAINT `fk_verification_record_ticket`
                                           FOREIGN KEY (`ticket_id`) REFERENCES `ticket` (`id`)
                                               ON DELETE RESTRICT ON UPDATE CASCADE,
                                       CONSTRAINT `fk_verification_record_verifier`
                                           FOREIGN KEY (`verifier_id`) REFERENCES `user_account` (`id`)
                                               ON DELETE RESTRICT ON UPDATE CASCADE,
                                       CONSTRAINT `chk_verification_record_result`
                                           CHECK (`result` IN ('SUCCESS', 'FAILED')),
                                       CONSTRAINT `chk_verification_record_failure_reason`
                                           CHECK (
                                               (`result` = 'SUCCESS' AND `failure_reason` IS NULL)
                                                   OR
                                               (`result` = 'FAILED' AND `failure_reason` IS NOT NULL)
                                               )
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '每一次核销请求的成功或失败记录';