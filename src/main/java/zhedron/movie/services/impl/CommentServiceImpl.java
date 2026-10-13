package zhedron.movie.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
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
import zhedron.movie.services.CommentService;
import zhedron.movie.services.MediaContentService;
import zhedron.movie.services.UserService;

import java.time.LocalDateTime;

@Service
@Slf4j
public class CommentServiceImpl implements CommentService {
    private final MediaContentRepository mediaContentRepository;
    private final CommentRepository commentRepository;

    private final CommentMapper commentMapper;
    private final MediaContentMapper mediaContentMapper;

    private final UserService userService;
    private final MediaContentService mediaContentService;

    public CommentServiceImpl(MediaContentRepository mediaContentRepository, CommentRepository commentRepository, CommentMapper commentMapper, MediaContentMapper mediaContentMapper, UserService userService, MediaContentService mediaContentService) {
        this.mediaContentRepository = mediaContentRepository;
        this.commentRepository = commentRepository;
        this.commentMapper = commentMapper;
        this.mediaContentMapper = mediaContentMapper;
        this.userService = userService;
        this.mediaContentService = mediaContentService;
    }

    @Override
    @CachePut(value = "comments", key = "#result.id()")
    public CommentResponse addComment(CommentRequest commentRequest, long mediaContentId) {
        User user = userService.getCurrentUser();

        MediaContentResponse mediaContentResponse = mediaContentService.findById(mediaContentId);

        MediaContent mediaContent = mediaContentMapper.toMediaContent(mediaContentResponse);

        Comment comment = new Comment();

        comment.setUser(user);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setEdited(false);
        comment.setText(commentRequest.getText());

        Comment savedComment = commentRepository.save(comment);

        mediaContent.getComments().add(savedComment);

        mediaContentRepository.save(mediaContent);

        return commentMapper.toCommentResponse(savedComment);
    }

    @Override
    @Caching(cacheable = {
            @Cacheable(value = "comments", key = "#commentId"),
            @Cacheable(value = "mediaContents", key = "#mediaContentId")
    })
    public CommentResponse findById(long commentId, long mediaContentId) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new CommentNotFoundException("Comment not found with " + commentId));

        MediaContentResponse mediaContentResponse = mediaContentService.findById(mediaContentId);

        MediaContent mediaContent = mediaContentMapper.toMediaContent(mediaContentResponse);

        if (!mediaContent.getComments().contains(comment)) {
            throw new CommentNotFoundException("Comment not found in Media with " + commentId);
        }

        return commentMapper.toCommentResponse(comment);
    }

    @Override
    @CacheEvict(value = "comments", key = "#id")
    public void deleteComment(long id) {
        if (!commentRepository.existsById(id)) {
            throw new CommentNotFoundException("Comment not found with " + id);
        }

        User user = userService.getCurrentUser();

        Comment comment = commentRepository.findById(id).orElseThrow(() -> new CommentNotFoundException("Comment not found with " + id));

        if (!comment.getUser().equals(user)) {
            throw new CommentDeleteAccessDeniedException("You are not allowed to delete this comment");
        }

        commentRepository.deleteById(id);
    }

    @Override
    @CacheEvict(value = "comments", key = "#id")
    public CommentResponse editComment(CommentRequest commentRequest, long id) {
        User currentUser = userService.getCurrentUser();

        Comment comment = commentRepository.findById(id).orElseThrow(() -> new CommentNotFoundException("Comment not found with " + id));

        if (!comment.getUser().equals(currentUser)) {
            throw new AccessDeniedException("You are not allowed to edit this comment");
        }

        comment.setEdited(true);
        comment.setText(commentRequest.getText());
        comment.setUpdatedAt(LocalDateTime.now());

        Comment editedComment = commentRepository.save(comment);

        log.info("Old comment {{}} changed to new comment {{}}", comment.getText(), editedComment.getText());

        return commentMapper.toCommentResponse(editedComment);
    }
}
