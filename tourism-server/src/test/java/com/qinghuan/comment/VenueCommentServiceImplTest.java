package com.qinghuan.comment;

import com.qinghuan.auth.context.UserContext;
import com.qinghuan.auth.model.LoginUser;
import com.qinghuan.pojo.dto.VenueCommentCreateDTO;
import com.qinghuan.pojo.entity.VenueComment;
import com.qinghuan.pojo.enums.AccountRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VenueCommentServiceImplTest {

    @Mock
    private VenueCommentMapper commentMapper;

    private VenueCommentServiceImpl commentService;

    @BeforeEach
    void setUp() {
        commentService = new VenueCommentServiceImpl(commentMapper);
        UserContext.set(new LoginUser(7L, "tourist", AccountRole.TOURIST, null));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void createComment_shouldBindCurrentUserAndTrimContent() {
        VenueCommentCreateDTO request = new VenueCommentCreateDTO();
        request.setContent("  很适合周末参观  ");
        when(commentMapper.existsEnabledVenue(1L)).thenReturn(true);
        when(commentMapper.insertComment(any(VenueComment.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, VenueComment.class).setId(12L);
            return 1;
        });

        Long commentId = commentService.createComment(1L, request);

        ArgumentCaptor<VenueComment> captor = ArgumentCaptor.forClass(VenueComment.class);
        verify(commentMapper).insertComment(captor.capture());
        assertEquals(12L, commentId);
        assertEquals(7L, captor.getValue().getUserId());
        assertEquals("很适合周末参观", captor.getValue().getContent());
    }

    @Test
    void likeComment_shouldNotIncreaseCountWhenAlreadyLiked() {
        when(commentMapper.existsComment(1L)).thenReturn(true);
        when(commentMapper.insertLike(1L, 7L)).thenReturn(0);

        commentService.likeComment(1L);

        verify(commentMapper, never()).increaseLikeCount(1L);
    }

    @Test
    void unlikeComment_shouldOnlyDecreaseCountWhenLikeWasRemoved() {
        when(commentMapper.existsComment(1L)).thenReturn(true);
        when(commentMapper.deleteLike(1L, 7L)).thenReturn(1);

        commentService.unlikeComment(1L);

        verify(commentMapper).decreaseLikeCount(1L);
    }
}
