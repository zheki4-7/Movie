package zhedron.movie.services;

import org.springframework.web.multipart.MultipartFile;
import zhedron.movie.dto.response.FilmResponse;

import java.io.IOException;

public interface FilmService {
    FilmResponse uploadFilm(MultipartFile video) throws IOException;

    void deleteById(long id);

    FilmResponse findById(long id);

    FilmResponse addSubtitleToFilm(long subtitleId, long filmId);

    FilmResponse removeSubtitleFromFilm(long subtitleId, long filmId);
}
