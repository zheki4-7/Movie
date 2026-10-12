package zhedron.movie.services;

import org.springframework.web.multipart.MultipartFile;
import zhedron.movie.dto.response.EpisodeResponse;
import zhedron.movie.dto.response.request.EpisodeCreateRequest;

import java.io.IOException;

public interface EpisodeService {
    EpisodeResponse uploadEpisode(MultipartFile video, EpisodeCreateRequest episodeCreateRequest) throws IOException;

    EpisodeResponse findById(long id);

    void deleteById(long id);

    EpisodeResponse addSubtitleToEpisode(long episodeId, long fileId);

    EpisodeResponse removeSubtitleFromEpisode(long episodeId, long fileId);
}
