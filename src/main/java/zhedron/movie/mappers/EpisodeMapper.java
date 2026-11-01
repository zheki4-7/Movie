package zhedron.movie.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import zhedron.movie.dto.response.EpisodeResponse;
import zhedron.movie.entity.Episode;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface EpisodeMapper {
    EpisodeResponse toEpisodeResponse(Episode episode);
}
