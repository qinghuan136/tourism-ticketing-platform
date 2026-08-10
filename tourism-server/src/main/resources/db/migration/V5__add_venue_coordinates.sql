ALTER TABLE venue
    ADD COLUMN longitude DECIMAL(10, 7) NULL COMMENT '景点经度',
    ADD COLUMN latitude DECIMAL(9, 7) NULL COMMENT '景点纬度';
