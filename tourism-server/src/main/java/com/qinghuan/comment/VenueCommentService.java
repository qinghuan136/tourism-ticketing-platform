package com.qinghuan.comment;

import com.qinghuan.pojo.dto.VenueCommentCreateDTO;
import com.qinghuan.pojo.dto.VenueCommentPageQueryDTO;
import com.qinghuan.pojo.vo.PageResult;
import com.qinghuan.pojo.vo.VenueCommentVO;

public interface VenueCommentService {

    /** 分页查询景点评论，固定按点赞数倒序。 */
    PageResult<VenueCommentVO> pageComments(Long venueId,
                                            VenueCommentPageQueryDTO queryDTO);

    /** 当前游客发表一条景点评论。 */
    Long createComment(Long venueId, VenueCommentCreateDTO createDTO);

    /** 当前游客点赞评论；重复点赞不重复累加。 */
    void likeComment(Long commentId);

    /** 当前游客取消点赞；未点赞时保持成功。 */
    void unlikeComment(Long commentId);
}
