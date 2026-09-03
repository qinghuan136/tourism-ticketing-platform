package com.qinghuan.comment;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qinghuan.auth.context.UserContext;
import com.qinghuan.common.exception.BusinessException;
import com.qinghuan.common.exception.ErrorCode;
import com.qinghuan.pojo.dto.VenueCommentCreateDTO;
import com.qinghuan.pojo.dto.VenueCommentPageQueryDTO;
import com.qinghuan.pojo.entity.VenueComment;
import com.qinghuan.pojo.vo.PageResult;
import com.qinghuan.pojo.vo.VenueCommentVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VenueCommentServiceImpl implements VenueCommentService {

    private final VenueCommentMapper commentMapper;

    public VenueCommentServiceImpl(VenueCommentMapper commentMapper) {
        this.commentMapper = commentMapper;
    }

    @Override
    public PageResult<VenueCommentVO> pageComments(
            Long venueId, VenueCommentPageQueryDTO queryDTO) {
        ensureEnabledVenue(venueId);

        PageHelper.startPage(queryDTO.getPage(), queryDTO.getSize());
        Page<VenueCommentVO> page = (Page<VenueCommentVO>)
                commentMapper.listByVenueId(venueId);
        return new PageResult<>(
                page.getResult(), page.getTotal(), page.getPageNum(), page.getPageSize());
    }

    @Override
    @Transactional
    public Long createComment(Long venueId, VenueCommentCreateDTO createDTO) {
        ensureEnabledVenue(venueId);

        VenueComment comment = new VenueComment();
        comment.setVenueId(venueId);
        comment.setUserId(UserContext.getUserId());
        comment.setContent(createDTO.getContent().trim());
        comment.setLikeCount(0);
        commentMapper.insertComment(comment);
        return comment.getId();
    }

    @Override
    @Transactional
    public void likeComment(Long commentId) {
        ensureCommentExists(commentId);

        // 只有首次插入点赞记录时，才增加评论的冗余点赞数。
        if (commentMapper.insertLike(commentId, UserContext.getUserId()) == 1) {
            commentMapper.increaseLikeCount(commentId);
        }
    }

    @Override
    @Transactional
    public void unlikeComment(Long commentId) {
        ensureCommentExists(commentId);

        // 删除到已有点赞记录时才递减，重复取消点赞保持幂等。
        if (commentMapper.deleteLike(commentId, UserContext.getUserId()) == 1) {
            commentMapper.decreaseLikeCount(commentId);
        }
    }

    private void ensureEnabledVenue(Long venueId) {
        if (!commentMapper.existsEnabledVenue(venueId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "景点不存在");
        }
    }

    private void ensureCommentExists(Long commentId) {
        if (!commentMapper.existsComment(commentId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "评论不存在");
        }
    }
}
