package zhedron.movie.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import zhedron.movie.dto.response.FilmResponse;
import zhedron.movie.entity.Film;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface FilmMapper {
    FilmResponse toFilmResponse(Film film);
}
