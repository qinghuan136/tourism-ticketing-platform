CREATE TABLE visitor_identity (
    visitor_id BIGINT UNSIGNED NOT NULL COMMENT '参观人ID',
    fingerprint CHAR(64) NOT NULL COMMENT '规范化证件身份SHA-256指纹',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (visitor_id),
    KEY idx_visitor_identity_fingerprint (fingerprint, visitor_id),

    CONSTRAINT fk_visitor_identity_visitor
        FOREIGN KEY (visitor_id) REFERENCES visitor(id)
            ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '参观人证件身份指纹';

-- 为已存在参观人补齐身份映射，规范规则与 VisitorFingerprintGenerator 保持一致。
INSERT INTO visitor_identity (visitor_id, fingerprint)
SELECT id,
       LOWER(SHA2(CONCAT(UPPER(TRIM(id_type)), ':', UPPER(TRIM(id_number))), 256))
FROM visitor;
