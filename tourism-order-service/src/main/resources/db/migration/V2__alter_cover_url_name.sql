ALTER TABLE venue
    CHANGE COLUMN cover_url cover_object_key VARCHAR(500) DEFAULT NULL COMMENT '封面对象键';