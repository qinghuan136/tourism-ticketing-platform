ALTER TABLE session_ticket_type
    ADD COLUMN allocated_quantity INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '该场次为票种分配的数量'
        AFTER sale_price,
    ADD COLUMN remaining_quantity INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '该场次票种剩余可售数量'
        AFTER allocated_quantity,
    ADD CONSTRAINT chk_session_ticket_type_remaining_quantity
        CHECK (remaining_quantity <= allocated_quantity);

-- 旧数据无法从共享余量准确反推各票种配额，因此安全地初始化为 0，
-- 需要运营者重新分配后才能继续销售。后续新增数据必须显式填写两个数量字段。
ALTER TABLE session_ticket_type
    ALTER COLUMN allocated_quantity DROP DEFAULT,
    ALTER COLUMN remaining_quantity DROP DEFAULT;
