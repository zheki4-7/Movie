package zhedron.movie.services;

import zhedron.movie.dto.response.SeasonResponse;
import zhedron.movie.dto.response.request.SeasonCreateRequest;

public interface SeasonService {
    SeasonResponse createSeason(SeasonCreateRequest seasonCreateRequest);

    SeasonResponse findById(long id);

    void deleteById(long id);

    SeasonResponse addEpisodeToSeason(long seasonId, long episodeId);

    void removeEpisodeFromSeason(long seasonId, long episodeId);
}
