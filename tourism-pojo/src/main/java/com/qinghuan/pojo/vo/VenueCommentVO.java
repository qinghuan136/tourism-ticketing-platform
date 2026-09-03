package com.qinghuan.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 景点评论列表展示数据。 */
@Getter
@Setter
@Schema(description = "景点评论")
public class VenueCommentVO {

    private Long id;
    private String authorName;
    private String content;
    private Integer likeCount;
    private LocalDateTime createdAt;
}
