package zhedron.movie.dto.response;

import java.util.List;

public record FilmResponse(long id, String videoUrl, String contentType, List<SubtitleResponse> subtitles) {
}
