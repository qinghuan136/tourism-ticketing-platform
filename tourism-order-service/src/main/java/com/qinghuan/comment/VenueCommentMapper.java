package com.qinghuan.comment;

import com.qinghuan.pojo.entity.VenueComment;
import com.qinghuan.pojo.vo.VenueCommentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface VenueCommentMapper {

    /** 判断景点是否仍可对游客展示。 */
    boolean existsEnabledVenue(@Param("venueId") Long venueId);

    /** 判断评论是否存在，供点赞和取消点赞校验。 */
    boolean existsComment(@Param("commentId") Long commentId);

    /** 新建评论并回填评论 ID。 */
    int insertComment(VenueComment comment);

    /** 按点赞数倒序查询指定景点的评论。 */
    List<VenueCommentVO> listByVenueId(@Param("venueId") Long venueId);

    /** 联合主键保证一位游客对同一评论只保留一条点赞记录。 */
    int insertLike(@Param("commentId") Long commentId, @Param("userId") Long userId);

    int increaseLikeCount(@Param("commentId") Long commentId);

    int deleteLike(@Param("commentId") Long commentId, @Param("userId") Long userId);

    int decreaseLikeCount(@Param("commentId") Long commentId);
}
