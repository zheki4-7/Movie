package zhedron.movie.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import zhedron.movie.dto.response.FilmResponse;
import zhedron.movie.entity.Film;
import zhedron.movie.entity.MediaContent;
import zhedron.movie.entity.Subtitle;
import zhedron.movie.exceptions.FilmNotFoundException;
import zhedron.movie.exceptions.SubtitleExistInFilmException;
import zhedron.movie.exceptions.SubtitleNotFoundException;
import zhedron.movie.mappers.FilmMapper;
import zhedron.movie.repository.FilmRepository;
import zhedron.movie.repository.MediaContentRepository;
import zhedron.movie.repository.SubtitleRepository;
import zhedron.movie.services.FilmService;
import zhedron.movie.services.SubtitleService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class FilmServiceImpl implements FilmService {
    private final MediaContentRepository mediaContentRepository;
    private final FilmRepository filmRepository;
    private final SubtitleRepository subtitleRepository;

    private final FilmMapper filmMapper;

    private final SubtitleService subtitleService;

    private final String DIRECTORY = "film/";

    public FilmServiceImpl(MediaContentRepository mediaContentRepository, FilmRepository filmRepository, SubtitleRepository subtitleRepository, FilmMapper filmMapper, SubtitleService subtitleService) {
        this.mediaContentRepository = mediaContentRepository;
        this.filmRepository = filmRepository;
        this.subtitleRepository = subtitleRepository;
        this.filmMapper = filmMapper;
        this.subtitleService = subtitleService;
    }

    @Override
    @CachePut(value = "films", key = "#result.id()")
    public FilmResponse uploadFilm(MultipartFile video) throws IOException {
        Film film = new Film();

        Path path = Paths.get(DIRECTORY);

        if (Files.notExists(path)) {
            Files.createDirectories(path);
        }

        String fileName = UUID.randomUUID().toString() + "_" + video.getOriginalFilename();

        Path videoFile = Paths.get(DIRECTORY).resolve(fileName).normalize();

        Files.copy(video.getInputStream(), videoFile, StandardCopyOption.REPLACE_EXISTING);

        film.setVideoUrl(fileName);
        film.setContentType(video.getContentType());

        Film savedFilm = filmRepository.save(film);

        return filmMapper.toFilmResponse(savedFilm);
    }

    @Override
    @CacheEvict(value = "films", key = "#id")
    public void deleteById(long id) {
        Film film = filmRepository.findById(id).orElseThrow(() -> new FilmNotFoundException("Film not found with id " + id));

        MediaContent mediaContent = mediaContentRepository.findByFilm(film);

        if (mediaContent != null && mediaContent.getFilm() != null) {
            mediaContent.setFilm(null);
            mediaContent.setDuration(0L);
        }

        filmRepository.deleteById(id);

        try {
            Path path = Paths.get(DIRECTORY).resolve(film.getVideoUrl()).normalize();

            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.error("Failed to delete file: {}", e.getMessage());
        }
    }

    @Override
    @Cacheable(value = "films", key = "#id")
    public FilmResponse findById(long id) {
        Film film = filmRepository.findById(id).orElseThrow(() -> new FilmNotFoundException("Film not found with " + id));

        return filmMapper.toFilmResponse(film);
    }

    @Override
    @Caching(
            cacheable = {
                    @Cacheable(
                            value = "subtitles", key = "#subtitleId"
                    ),
                    @Cacheable(
                            value = "films", key = "#filmId"
                    )
            }
    )
    public FilmResponse addSubtitleToFilm(long subtitleId, long filmId) {
        Film film = filmRepository.findById(filmId).orElseThrow(() -> new FilmNotFoundException("Film not found with id " + filmId));

        Subtitle subtitle = subtitleService.findById(subtitleId);

        if (film.getSubtitles().contains(subtitle)) {
            throw new SubtitleExistInFilmException("Subtitle exists in Film");
        }

        subtitle.setReleased(true);
        subtitle.setFilm(film);

        subtitleRepository.save(subtitle);

        film.getSubtitles().add(subtitle);

        Film addedSubtitle = filmRepository.save(film);

        return filmMapper.toFilmResponse(addedSubtitle);
    }

    @Override
    @Caching(
            cacheable = {
                    @Cacheable(
                            value = "subtitles", key = "#subtitleId"
                    ),
                    @Cacheable(
                            value = "films", key = "#filmId"
                    )
            }
    )
    public FilmResponse removeSubtitleFromFilm(long subtitleId, long filmId) {
        Film film = filmRepository.findById(filmId).orElseThrow(() -> new FilmNotFoundException("Film not found with id " + filmId));

        Subtitle subtitle = subtitleService.findById(subtitleId);

        if (!film.getSubtitles().contains(subtitle)) {
            throw new SubtitleNotFoundException("Subtitle not found in Film");
        }

        film.getSubtitles().remove(subtitle);

        Film removedFilm = filmRepository.save(film);

        return filmMapper.toFilmResponse(removedFilm);
    }
}
