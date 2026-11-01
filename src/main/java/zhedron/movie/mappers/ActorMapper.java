package zhedron.movie.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import zhedron.movie.dto.response.ActorResponse;
import zhedron.movie.entity.Actor;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {MediaContentMapper.class})
public interface ActorMapper {
    ActorResponse toActorResponse(Actor actor);
}
