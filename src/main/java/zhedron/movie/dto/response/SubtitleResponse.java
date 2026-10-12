package zhedron.movie.dto.response;

public record SubtitleResponse(long id, String srtName, boolean released, long filmId, long episodeId) {
}
