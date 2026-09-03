package com.qinghuan.pojo.entity;

import lombok.Getter;
import lombok.Setter;

/** 游客发表的景点评论。 */
@Getter
@Setter
public class VenueComment extends BaseEntity {

    private Long venueId;
    private Long userId;
    private String content;
    private Integer likeCount;
}
