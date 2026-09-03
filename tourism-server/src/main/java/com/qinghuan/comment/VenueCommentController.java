package com.qinghuan.comment;

import com.qinghuan.annotation.RequireRole;
import com.qinghuan.common.response.ApiResponse;
import com.qinghuan.pojo.dto.VenueCommentCreateDTO;
import com.qinghuan.pojo.dto.VenueCommentPageQueryDTO;
import com.qinghuan.pojo.enums.AccountRole;
import com.qinghuan.pojo.vo.PageResult;
import com.qinghuan.pojo.vo.VenueCommentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@Tag(name = "景点评论", description = "游客浏览景点评论、发表评论和点赞")
public class VenueCommentController {

    private final VenueCommentService commentService;

    public VenueCommentController(VenueCommentService commentService) {
        this.commentService = commentService;
    }

    /** 公开展示评论，固定按点赞数倒序。 */
    @GetMapping("/public/venues/{venueId}/comments")
    @Operation(summary = "分页查询景点评论")
    public ApiResponse<PageResult<VenueCommentVO>> pageComments(
            @PathVariable @Positive(message = "景点ID必须为正数") Long venueId,
            @ParameterObject @Valid VenueCommentPageQueryDTO queryDTO) {
        return ApiResponse.success(commentService.pageComments(venueId, queryDTO));
    }

    /** 评论者由 JWT 上下文确定，前端不传递 userId。 */
    @PostMapping("/tourist/venues/{venueId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    @RequireRole(AccountRole.TOURIST)
    @Operation(summary = "发表景点评论")
    @SecurityRequirement(name = "BearerAuth")
    public ApiResponse<Long> createComment(
            @PathVariable @Positive(message = "景点ID必须为正数") Long venueId,
            @Valid @RequestBody VenueCommentCreateDTO createDTO) {
        return ApiResponse.success(commentService.createComment(venueId, createDTO));
    }

    /** 同一游客重复点赞时不重复累加。 */
    @PutMapping("/tourist/comments/{commentId}/like")
    @RequireRole(AccountRole.TOURIST)
    @Operation(summary = "点赞景点评论")
    @SecurityRequirement(name = "BearerAuth")
    public ApiResponse<Void> likeComment(
            @PathVariable @Positive(message = "评论ID必须为正数") Long commentId) {
        commentService.likeComment(commentId);
        return ApiResponse.success();
    }

    /** 取消未存在的点赞记录时不报错。 */
    @DeleteMapping("/tourist/comments/{commentId}/like")
    @RequireRole(AccountRole.TOURIST)
    @Operation(summary = "取消点赞景点评论")
    @SecurityRequirement(name = "BearerAuth")
    public ApiResponse<Void> unlikeComment(
            @PathVariable @Positive(message = "评论ID必须为正数") Long commentId) {
        commentService.unlikeComment(commentId);
        return ApiResponse.success();
    }
}
