package zhedron.movie.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import zhedron.movie.dto.response.FavoriteResponse;
import zhedron.movie.entity.Favorite;


@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = MediaContentMapper.class)
public interface FavoriteMapper {
    FavoriteResponse toFavoriteResponse(Favorite favorite);
}
