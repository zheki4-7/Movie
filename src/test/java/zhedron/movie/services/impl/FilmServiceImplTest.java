package zhedron.movie.services.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import zhedron.movie.dto.response.FilmResponse;
import zhedron.movie.entity.Film;
import zhedron.movie.entity.Subtitle;
import zhedron.movie.mappers.FilmMapper;
import zhedron.movie.repository.FilmRepository;
import zhedron.movie.repository.MediaContentRepository;
import zhedron.movie.repository.SubtitleRepository;
import zhedron.movie.services.SubtitleService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilmServiceImplTest {
    @Mock
    private MediaContentRepository mediaContentRepository;
    @Mock
    private FilmRepository filmRepository;
    @Mock
    private SubtitleRepository subtitleRepository;
    @Mock
    private FilmMapper filmMapper;
    @Mock
    private SubtitleService subtitleService;
    @InjectMocks
    private FilmServiceImpl filmService;

    @Test
    void addSubtitleLinksAndReleasesSubtitle() {
        Film film = new Film();
        film.setId(6L);
        film.setSubtitles(new ArrayList<>());
        Subtitle subtitle = new Subtitle();
        subtitle.setId(3);
        FilmResponse expected = new FilmResponse(6L, "film.mp4", "video/mp4", List.of());
        when(filmRepository.findById(6L)).thenReturn(Optional.of(film));
        when(subtitleService.findById(3L)).thenReturn(subtitle);
        when(filmRepository.save(film)).thenReturn(film);
        when(filmMapper.toFilmResponse(film)).thenReturn(expected);

        assertEquals(expected, filmService.addSubtitleToFilm(3L, 6L));

        assertTrue(subtitle.isReleased());
        assertEquals(film, subtitle.getFilm());
        assertTrue(film.getSubtitles().contains(subtitle));
        verify(subtitleRepository).save(subtitle);
    }

    @Test
    void removeSubtitleUnlinksAssociation() {
        Film film = new Film();
        film.setId(6L);
        Subtitle subtitle = new Subtitle();
        subtitle.setId(3);
        film.setSubtitles(new ArrayList<>(List.of(subtitle)));
        FilmResponse expected = new FilmResponse(6L, "film.mp4", "video/mp4", List.of());
        when(filmRepository.findById(6L)).thenReturn(Optional.of(film));
        when(subtitleService.findById(3L)).thenReturn(subtitle);
        when(filmRepository.save(film)).thenReturn(film);
        when(filmMapper.toFilmResponse(film)).thenReturn(expected);

        assertEquals(expected, filmService.removeSubtitleFromFilm(3L, 6L));

        assertTrue(film.getSubtitles().isEmpty());
        verify(filmRepository).save(film);
    }
}
