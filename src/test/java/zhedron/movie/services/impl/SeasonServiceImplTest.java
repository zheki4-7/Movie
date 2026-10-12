package zhedron.movie.services.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import zhedron.movie.dto.response.EpisodeResponse;
import zhedron.movie.dto.response.SeasonResponse;
import zhedron.movie.dto.response.request.SeasonCreateRequest;
import zhedron.movie.entity.Episode;
import zhedron.movie.entity.Season;
import zhedron.movie.exceptions.EpisodeInSeasonNotContainException;
import zhedron.movie.exceptions.SeasonNotFoundException;
import zhedron.movie.mappers.SeasonMapper;
import zhedron.movie.repository.EpisodeRepository;
import zhedron.movie.repository.SeasonRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeasonServiceImplTest {
    @Mock
    private SeasonRepository seasonRepository;
    @Mock
    private EpisodeRepository episodeRepository;
    @Mock
    private SeasonMapper seasonMapper;
    @InjectMocks
    private SeasonServiceImpl seasonService;

    @Test
    void createSeasonPersistsSeasonNumber() {
        SeasonCreateRequest request = new SeasonCreateRequest();
        request.setSeasonNumber(3);
        Season savedSeason = new Season();
        savedSeason.setId(9L);
        savedSeason.setSeasonNumber(3);
        SeasonResponse expectedResponse = new SeasonResponse(9L, null, 3);
        when(seasonRepository.save(any(Season.class))).thenReturn(savedSeason);
        when(seasonMapper.toSeasonResponse(savedSeason)).thenReturn(expectedResponse);

        SeasonResponse response = seasonService.createSeason(request);

        assertEquals(expectedResponse, response);
        verify(seasonRepository).save(any(Season.class));
    }

    @Test
    void addEpisodeToSeasonAddsEpisodeAndSavesSeason() {
        Season season = new Season();
        season.setId(4L);
        season.setEpisodes(new ArrayList<>());
        Episode episode = new Episode();
        episode.setId(12L);
        SeasonResponse expectedResponse = new SeasonResponse(4L, List.of(new EpisodeResponse(12L, null, 0, null, List.of())), 1);
        when(seasonRepository.findById(4L)).thenReturn(Optional.of(season));
        when(episodeRepository.findById(12L)).thenReturn(Optional.of(episode));
        when(seasonMapper.toSeasonResponse(season)).thenReturn(expectedResponse);

        SeasonResponse response = seasonService.addEpisodeToSeason(4L, 12L);

        assertEquals(expectedResponse, response);
        assertEquals(1, season.getEpisodes().size());
        assertEquals(episode, season.getEpisodes().get(0));
        verify(seasonRepository).save(season);
    }

    @Test
    void deleteEpisodeFromSeasonThrowsWhenEpisodeIsNotLinked() {
        Season season = new Season();
        season.setEpisodes(new ArrayList<>());
        Episode episode = new Episode();
        episode.setId(5L);
        when(seasonRepository.findById(1L)).thenReturn(Optional.of(season));
        when(episodeRepository.findById(5L)).thenReturn(Optional.of(episode));

        assertThrows(EpisodeInSeasonNotContainException.class, () -> seasonService.deleteEpisodeFromSeason(1L, 5L));
    }

    @Test
    void deleteByIdThrowsWhenSeasonDoesNotExist() {
        when(seasonRepository.existsById(99L)).thenReturn(false);

        assertThrows(SeasonNotFoundException.class, () -> seasonService.deleteById(99L));
    }
}
