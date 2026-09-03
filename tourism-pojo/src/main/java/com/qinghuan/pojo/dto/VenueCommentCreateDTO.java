package com.qinghuan.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** 游客发表景点评论的请求参数。 */
@Getter
@Setter
@Schema(description = "发表景点评论的参数")
public class VenueCommentCreateDTO {

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论内容不能超过500个字符")
    @Schema(description = "评论内容", example = "展馆内容丰富，适合亲子游览。")
    private String content;
}
