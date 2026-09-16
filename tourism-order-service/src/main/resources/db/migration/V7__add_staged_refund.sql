-- 保存退款意图和稳定的商户退款单号，供超时对账使用。
ALTER TABLE `booking_order`
    ADD COLUMN `refund_no` VARCHAR(64) NULL COMMENT '商户退款单号' AFTER `payment_no`,
    ADD COLUMN `refund_requested_at` DATETIME NULL COMMENT '退款申请时间' AFTER `completed_at`,
    ADD UNIQUE KEY `uk_booking_order_refund_no` (`refund_no`),
    ADD KEY `idx_booking_order_refunding` (`status`, `refund_requested_at`);

-- REFUNDING 表示已保存退款意图，但第三方结果尚未确认。
ALTER TABLE `booking_order`
    DROP CHECK `chk_booking_order_status`,
    ADD CONSTRAINT `chk_booking_order_status`
        CHECK (`status` IN (
            'PENDING_PAYMENT', 'PAID', 'REFUNDING', 'CANCELLED',
            'CLOSED', 'COMPLETED', 'REFUNDED'
        ));

-- 退款申请落库后先冻结票券，避免等待平台结果期间发生核销。
ALTER TABLE `ticket`
    DROP CHECK `chk_ticket_status`,
    ADD CONSTRAINT `chk_ticket_status`
        CHECK (`status` IN ('VALID', 'REFUNDING', 'USED', 'VOID', 'EXPIRED'));
