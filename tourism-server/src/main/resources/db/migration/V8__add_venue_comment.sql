CREATE TABLE venue_comment (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '评论ID',
    venue_id BIGINT UNSIGNED NOT NULL COMMENT '景点ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '评论游客ID',
    content VARCHAR(500) NOT NULL COMMENT '评论内容',
    like_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '点赞数量',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',

    PRIMARY KEY (id),
    KEY idx_venue_comment_venue_likes (venue_id, like_count DESC, id DESC),
    CONSTRAINT fk_venue_comment_venue
        FOREIGN KEY (venue_id) REFERENCES venue (id)
            ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_venue_comment_user
        FOREIGN KEY (user_id) REFERENCES user_account (id)
            ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '游客对景点发表的评论';

CREATE TABLE venue_comment_like (
    comment_id BIGINT UNSIGNED NOT NULL COMMENT '评论ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '点赞游客ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',

    PRIMARY KEY (comment_id, user_id),
    KEY idx_venue_comment_like_user (user_id),
    CONSTRAINT fk_venue_comment_like_comment
        FOREIGN KEY (comment_id) REFERENCES venue_comment (id)
            ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_venue_comment_like_user
        FOREIGN KEY (user_id) REFERENCES user_account (id)
            ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '游客对景点评论的点赞记录';
