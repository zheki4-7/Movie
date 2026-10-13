package zhedron.movie.services.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import zhedron.movie.dto.response.CommentResponse;
import zhedron.movie.dto.response.MediaContentResponse;
import zhedron.movie.dto.response.request.CommentRequest;
import zhedron.movie.entity.Comment;
import zhedron.movie.entity.MediaContent;
import zhedron.movie.entity.User;
import zhedron.movie.exceptions.AccessDeniedException;
import zhedron.movie.exceptions.CommentDeleteAccessDeniedException;
import zhedron.movie.exceptions.CommentNotFoundException;
import zhedron.movie.mappers.CommentMapper;
import zhedron.movie.mappers.MediaContentMapper;
import zhedron.movie.repository.CommentRepository;
import zhedron.movie.repository.MediaContentRepository;
import zhedron.movie.services.MediaContentService;
import zhedron.movie.services.UserService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {
    @Mock
    private MediaContentRepository mediaContentRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private CommentMapper commentMapper;
    @Mock
    private MediaContentMapper mediaContentMapper;
    @Mock
    private UserService userService;
    @Mock
    private MediaContentService mediaContentService;
    @InjectMocks
    private CommentServiceImpl commentService;

    @Test
    void addCommentSavesCommentAndAttachesItToMediaContent() {
        User user = new User();
        user.setId(3L);
        CommentRequest request = new CommentRequest();
        request.setText("Great film");
        MediaContentResponse mediaResponse = new MediaContentResponse(8L, "Title", "Description", null, 0, null, null, null, null, null, null, null, List.of(), List.of());
        MediaContent mediaContent = new MediaContent();
        Comment savedComment = new Comment();
        savedComment.setId(17L);
        savedComment.setText("Great film");
        CommentResponse expectedResponse = new CommentResponse(17L, "Great film", null, null, null, false);
        when(userService.getCurrentUser()).thenReturn(user);
        when(mediaContentService.findById(8L)).thenReturn(mediaResponse);
        when(mediaContentMapper.toMediaContent(mediaResponse)).thenReturn(mediaContent);
        when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);
        when(commentMapper.toCommentResponse(savedComment)).thenReturn(expectedResponse);

        CommentResponse response = commentService.addComment(request, 8L);

        ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(commentCaptor.capture());
        assertSame(user, commentCaptor.getValue().getUser());
        assertEquals("Great film", commentCaptor.getValue().getText());
        assertNotNull(commentCaptor.getValue().getCreatedAt());
        assertTrue(mediaContent.getComments().contains(savedComment));
        verify(mediaContentRepository).save(mediaContent);
        assertSame(expectedResponse, response);
    }

    @Test
    void findByIdThrowsWhenCommentIsNotAttachedToMediaContent() {
        Comment comment = new Comment();
        comment.setId(6L);
        MediaContentResponse mediaResponse = new MediaContentResponse(1L, "Title", "Description", null, 0, null, null, null, null, null, null, null, List.of(), List.of());
        MediaContent mediaContent = new MediaContent();
        mediaContent.setComments(new ArrayList<>());
        when(commentRepository.findById(6L)).thenReturn(Optional.of(comment));
        when(mediaContentService.findById(1L)).thenReturn(mediaResponse);
        when(mediaContentMapper.toMediaContent(mediaResponse)).thenReturn(mediaContent);

        assertThrows(CommentNotFoundException.class, () -> commentService.findById(6L, 1L));
    }

    @Test
    void deleteCommentRejectsDifferentCurrentUser() {
        User owner = new User();
        owner.setId(1L);
        User current = new User();
        current.setId(2L);
        Comment comment = new Comment();
        comment.setId(10L);
        comment.setUser(owner);
        when(commentRepository.existsById(10L)).thenReturn(true);
        when(userService.getCurrentUser()).thenReturn(current);
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));

        assertThrows(CommentDeleteAccessDeniedException.class, () -> commentService.deleteComment(10L));

        verify(commentRepository, never()).deleteById(10L);
    }

    @Test
    void editCommentMarksCommentEdited() {
        CommentRequest request = new CommentRequest();
        request.setText("Updated");
        User owner = new User();
        owner.setId(4L);
        when(userService.getCurrentUser()).thenReturn(owner);
        Comment comment = new Comment();
        comment.setId(14L);
        comment.setText("Original");
        comment.setUser(owner);
        CommentResponse expectedResponse = new CommentResponse(14L, "Updated", null, null, null, true);
        when(commentRepository.findById(14L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(comment)).thenReturn(comment);
        when(commentMapper.toCommentResponse(comment)).thenReturn(expectedResponse);

        CommentResponse response = commentService.editComment(request, 14L);

        assertTrue(comment.isEdited());
        assertEquals("Updated", comment.getText());
        assertNotNull(comment.getUpdatedAt());
        assertSame(expectedResponse, response);
    }

    @Test
    void editCommentRejectsDifferentCurrentUser() {
        User owner = new User();
        owner.setId(4L);
        User currentUser = new User();
        currentUser.setId(9L);
        Comment comment = new Comment();
        comment.setId(14L);
        comment.setUser(owner);
        CommentRequest request = new CommentRequest();
        request.setText("Updated");
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(commentRepository.findById(14L)).thenReturn(Optional.of(comment));

        assertThrows(AccessDeniedException.class, () -> commentService.editComment(request, 14L));

        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void editCommentThrowsWhenCommentIsMissing() {
        when(userService.getCurrentUser()).thenReturn(new User());
        when(commentRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(CommentNotFoundException.class,
                () -> commentService.editComment(new CommentRequest(), 404L));
    }
}
