-- 服务拆分后，订单和优惠券查询只读取各自拥有的表。
ALTER TABLE booking_order
    ADD COLUMN venue_id BIGINT NULL AFTER session_id,
    ADD COLUMN venue_name_snapshot VARCHAR(128) NULL AFTER venue_id,
    ADD COLUMN venue_address_snapshot VARCHAR(255) NULL AFTER venue_name_snapshot,
    ADD COLUMN visit_date DATE NULL AFTER venue_address_snapshot,
    ADD COLUMN start_time TIME NULL AFTER visit_date,
    ADD COLUMN end_time TIME NULL AFTER start_time,
    ADD COLUMN purchaser_name_snapshot VARCHAR(64) NULL AFTER end_time,
    ADD COLUMN purchaser_phone_snapshot VARCHAR(32) NULL AFTER purchaser_name_snapshot,
    ADD KEY idx_booking_order_venue_visit_status (venue_id, visit_date, status);

ALTER TABLE booking_order_item
    ADD COLUMN visitor_fingerprint CHAR(64) NULL AFTER visitor_id_number,
    ADD KEY idx_booking_order_item_fingerprint (visitor_fingerprint);

ALTER TABLE verification_record
    ADD COLUMN verifier_name_snapshot VARCHAR(64) NULL AFTER verifier_id;

ALTER TABLE coupon_activity
    ADD COLUMN venue_name_snapshot VARCHAR(128) NULL AFTER venue_id;

ALTER TABLE user_coupon
    ADD COLUMN venue_name_snapshot VARCHAR(128) NULL AFTER venue_id;

-- 共享数据库阶段对存量记录做一次性回填；后续运行时 SQL 不再关联这些跨域表。
UPDATE booking_order bo
LEFT JOIN admission_session s ON s.id = bo.session_id
LEFT JOIN venue v ON v.id = s.venue_id
LEFT JOIN user_account u ON u.id = bo.user_id
SET bo.venue_id = s.venue_id,
    bo.venue_name_snapshot = v.name,
    bo.venue_address_snapshot = v.address,
    bo.visit_date = s.visit_date,
    bo.start_time = s.start_time,
    bo.end_time = s.end_time,
    bo.purchaser_name_snapshot = u.display_name,
    bo.purchaser_phone_snapshot = u.phone
WHERE bo.venue_id IS NULL;

UPDATE booking_order_item item
JOIN visitor_identity identity_snapshot ON identity_snapshot.visitor_id = item.visitor_id
SET item.visitor_fingerprint = identity_snapshot.fingerprint
WHERE item.visitor_fingerprint IS NULL;

UPDATE verification_record vr
JOIN user_account verifier ON verifier.id = vr.verifier_id
SET vr.verifier_name_snapshot = verifier.display_name
WHERE vr.verifier_name_snapshot IS NULL;

UPDATE coupon_activity ca
JOIN venue v ON v.id = ca.venue_id
SET ca.venue_name_snapshot = v.name
WHERE ca.venue_name_snapshot IS NULL;

UPDATE user_coupon uc
JOIN venue v ON v.id = uc.venue_id
SET uc.venue_name_snapshot = v.name
WHERE uc.venue_name_snapshot IS NULL;
