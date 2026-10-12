package zhedron.movie.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import zhedron.movie.dto.response.SubtitleResponse;
import zhedron.movie.entity.Subtitle;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SubtitleMapper {
    @Mapping(target = "filmId", expression = "java(subtitle.getFilm() != null ? subtitle.getFilm().getId() : 0)")
    @Mapping(target = "episodeId", expression = "java(subtitle.getEpisode() != null ? subtitle.getEpisode().getId() : 0)")
    SubtitleResponse toSubtitleResponse(Subtitle subtitle);
}
