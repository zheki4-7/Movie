package zhedron.movie.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import zhedron.movie.dto.response.SeasonResponse;
import zhedron.movie.dto.response.request.SeasonCreateRequest;
import zhedron.movie.entity.Episode;
import zhedron.movie.entity.Season;
import zhedron.movie.exceptions.EpisodeInSeasonNotContainException;
import zhedron.movie.exceptions.EpisodeNotFoundException;
import zhedron.movie.exceptions.SeasonNotFoundException;
import zhedron.movie.mappers.SeasonMapper;
import zhedron.movie.repository.EpisodeRepository;
import zhedron.movie.repository.SeasonRepository;
import zhedron.movie.services.SeasonService;

import java.util.List;

@Service
@Slf4j
public class SeasonServiceImpl implements SeasonService {
    private final SeasonRepository seasonRepository;
    private final EpisodeRepository episodeRepository;

    private final SeasonMapper seasonMapper;

    public SeasonServiceImpl(SeasonRepository seasonRepository, SeasonMapper seasonMapper, EpisodeRepository episodeRepository) {
        this.seasonRepository = seasonRepository;
        this.episodeRepository = episodeRepository;
        this.seasonMapper = seasonMapper;
    }

    @Override
    @CachePut(value = "seasons", key = "#result.id()")
    public SeasonResponse createSeason(SeasonCreateRequest seasonCreateRequest) {
        Season season = new Season();

        season.setSeasonNumber(seasonCreateRequest.getSeasonNumber());

        Season savedSeason = seasonRepository.save(season);

        return seasonMapper.toSeasonResponse(savedSeason);
    }

    @Override
    @Cacheable(value = "seasons", key = "#id")
    public SeasonResponse findById(long id) {
        Season season = seasonRepository.findById(id).orElseThrow(() -> new SeasonNotFoundException("Season not found with " + id));

        return seasonMapper.toSeasonResponse(season);
    }

    @Override
    @CacheEvict(value = "seasons", key = "#id")
    public void deleteById(long id) {
        if (!seasonRepository.existsById(id)) {
            throw new SeasonNotFoundException("Season not found with " + id);
        }

        seasonRepository.deleteById(id);
    }

    @Override
    @Caching(cacheable = {
            @Cacheable(value = "seasons", key = "#seasonId"),
            @Cacheable(value = "episodes", key = "#episodeId")
    })
    public SeasonResponse addEpisodeToSeason(long seasonId, long episodeId) {
        Season season = seasonRepository.findById(seasonId).orElseThrow(() -> new SeasonNotFoundException("Season not found with " + seasonId));
        Episode episode = episodeRepository.findById(episodeId).orElseThrow(() -> new EpisodeNotFoundException("Episode not found with " + episodeId));

        List<Episode> episodes = season.getEpisodes();

        episodes.add(episode);

        season.setEpisodes(episodes);

        seasonRepository.save(season);

        log.info("Added episode {} to season {}", episodeId, seasonId);

        return seasonMapper.toSeasonResponse(season);
    }

    @Override
    @Caching(cacheable = {
            @Cacheable(value = "seasons", key = "#seasonId"),
            @Cacheable(value = "episodes", key = "#episodeId")
    })
    public void removeEpisodeFromSeason(long seasonId, long episodeId) {
        Season season = seasonRepository.findById(seasonId).orElseThrow(() -> new SeasonNotFoundException("Season not found with " + seasonId));
        Episode episode = episodeRepository.findById(episodeId).orElseThrow(() -> new EpisodeNotFoundException("Episode not found with " + episodeId));

        if (!season.getEpisodes().contains(episode)) {
            throw new EpisodeInSeasonNotContainException("Episode not found in Season");
        }

        season.getEpisodes().remove(episode);

        seasonRepository.save(season);
    }
}
