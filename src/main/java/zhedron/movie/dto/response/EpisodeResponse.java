package zhedron.movie.dto.response;

import java.util.List;

public record EpisodeResponse(long id, String episodeUrl, long episodeNumber, String contentType, List<SubtitleResponse> subtitles) {
}
