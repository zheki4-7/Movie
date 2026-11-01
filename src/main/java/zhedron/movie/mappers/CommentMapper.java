package zhedron.movie.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import zhedron.movie.dto.response.CommentResponse;
import zhedron.movie.entity.Comment;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CommentMapper {
    CommentResponse toCommentResponse(Comment comment);
}
