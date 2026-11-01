package zhedron.movie.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import zhedron.movie.dto.response.SeasonResponse;
import zhedron.movie.entity.Season;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = EpisodeMapper.class)
public interface SeasonMapper {
    SeasonResponse toSeasonResponse(Season season);
}
