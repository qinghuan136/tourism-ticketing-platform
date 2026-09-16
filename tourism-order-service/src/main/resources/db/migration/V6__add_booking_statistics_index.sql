-- 运营趋势统计先按景点找到场次，再按 session_id 关联订单。
-- 把统计所需字段放入同一索引，减少读取订单主表数据页的次数。
CREATE INDEX idx_booking_order_session_paid_cover
    ON booking_order (session_id, paid_at, total_amount, quantity);
