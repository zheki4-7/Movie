package zhedron.movie.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.mp4parser.IsoFile;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import zhedron.movie.dto.response.MediaContentResponse;
import zhedron.movie.dto.response.PaginatedResponse;
import zhedron.movie.dto.response.request.MediaContentRequest;
import zhedron.movie.entity.*;
import zhedron.movie.enums.Status;
import zhedron.movie.exceptions.*;
import zhedron.movie.mappers.MediaContentMapper;
import zhedron.movie.repository.FavoriteRepository;
import zhedron.movie.repository.FilmRepository;
import zhedron.movie.repository.MediaContentRepository;
import zhedron.movie.repository.SeasonRepository;
import zhedron.movie.services.ActorService;
import zhedron.movie.services.MediaContentService;
import zhedron.movie.services.UserService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class MediaContentServiceImpl implements MediaContentService {
    private final MediaContentRepository mediaContentRepository;
    private final FilmRepository filmRepository;
    private final SeasonRepository seasonRepository;
    private final FavoriteRepository favoriteRepository;

    private final MediaContentMapper mediaContentMapper;

    private final String DIRECTORY = "cover_art/";

    private final UserService userService;
    private final ActorService actorService;

    public MediaContentServiceImpl(MediaContentRepository mediaContentRepository, FilmRepository filmRepository, SeasonRepository seasonRepository, FavoriteRepository favoriteRepository, MediaContentMapper mediaContentMapper, UserService userService, ActorService actorService) {
        this.mediaContentRepository = mediaContentRepository;
        this.filmRepository = filmRepository;
        this.seasonRepository = seasonRepository;
        this.favoriteRepository = favoriteRepository;
        this.mediaContentMapper = mediaContentMapper;
        this.userService = userService;
        this.actorService = actorService;
    }

    @Override
    @CachePut(value = "mediaContents", key = "#result.id()")
    public MediaContentResponse createMediaContent(MediaContentRequest mediaContentRequest, List<MultipartFile> images) throws IOException {
        Path path = Paths.get(DIRECTORY);

        if (Files.notExists(path)) {
            Files.createDirectories(path);
        }

        MediaContent mediaContent = new MediaContent();

        List<String> fileNames = new ArrayList<>();

        List<String> contentTypes = new ArrayList<>();

        for (MultipartFile image : images) {
            String fileName = UUID.randomUUID().toString() + "_" + image.getOriginalFilename();

            fileNames.add(fileName);

            contentTypes.add(image.getContentType());

            Path imageFile = path.resolve(fileName).normalize();

            Files.copy(image.getInputStream(), imageFile, StandardCopyOption.REPLACE_EXISTING);
        }

        mediaContent.setCoverArts(fileNames);
        mediaContent.setDescription(mediaContentRequest.getDescription());
        mediaContent.setTitle(mediaContentRequest.getTitle());
        mediaContent.setReleaseDate(mediaContentRequest.getReleaseDate());
        mediaContent.setUser(userService.getCurrentUser());
        mediaContent.setTrailerUrl(mediaContentRequest.getTrailerUrl());
        mediaContent.setStatus(Status.PRIVATE);
        mediaContent.setCompanyName(mediaContentRequest.getCompanyName());
        mediaContent.setUser(userService.getCurrentUser());
        mediaContent.setContentTypes(contentTypes);

        MediaContent savedMediaContent = mediaContentRepository.save(mediaContent);

        return mediaContentMapper.toMediaContentResponse(savedMediaContent);
    }

    @Override
    @Cacheable(value = "mediaContents", key = "#id")
    public MediaContentResponse findById(long id) {
        MediaContent mediaContent = mediaContentRepository.findById(id).orElseThrow(() -> new MediaContentNotFoundException("Media Content not found with id " + id));

        if (!mediaContent.getSeasons().isEmpty()) {
            for (Season season : mediaContent.getSeasons()) {
                if (!season.getEpisodes().isEmpty()) {
                    for (Episode episode : season.getEpisodes()) {
                        try (IsoFile isoFile = new IsoFile(new File("episode/" + episode.getEpisodeUrl()))) {
                            long duration = isoFile.getMovieBox().getMovieHeaderBox().getDuration() / isoFile.getMovieBox().getMovieHeaderBox().getTimescale();

                            if (mediaContent.getDuration() != duration) {
                                mediaContent.setDuration(duration);

                                mediaContent = mediaContentRepository.save(mediaContent);
                            }
                        } catch (IOException e) {
                            log.error("Failed to update duration {}", e.getMessage());
                        }
                    }
                } else {
                    mediaContent.setDuration(0L);

                    mediaContent = mediaContentRepository.save(mediaContent);
                }
            }
        }

        return mediaContentMapper.toMediaContentResponse(mediaContent);
    }

    @Override
    @CacheEvict(value = "mediaContents", key = "#id")
    public void deleteById(long id) {
        MediaContentResponse mediaContentResponseFound = findById(id);

        MediaContent mediaContentFound = mediaContentMapper.toMediaContent(mediaContentResponseFound);

        Favorite favorite = favoriteRepository.findByMediaContentId(mediaContentFound.getId());

        favorite.getMediaContents().remove(mediaContentFound);

        mediaContentRepository.deleteById(id);

        log.info("Media Content with {} successfully deleted", id);
    }

    @Override
    @Cacheable(value = "mediaContents", key = "#id")
    public MediaContentResponse changeVisibility(Status status, long id) {
        MediaContent mediaContent = mediaContentRepository.findById(id).orElseThrow(() -> new MediaContentNotFoundException("Media Content not found with id " + id));

        mediaContent.setStatus(status);

        MediaContent savedMediaContent = mediaContentRepository.save(mediaContent);

        return mediaContentMapper.toMediaContentResponse(savedMediaContent);
    }

    @Override
    @Caching(cacheable = {
            @Cacheable(value = "mediaContents", key = "#mediaContentId"),
            @Cacheable(value = "films", key = "#filmId")
    })
    public MediaContentResponse addFilmToMediaContent(long mediaContentId, long filmId) {
        if (mediaContentRepository.existsByIdAndFilmIsNotNullOrSeasonsIsNotEmpty(mediaContentId)) {
            throw new MediaContentExistsVideoOrSeasonException("Episodes/Film already exists in Media Content");
        }

        MediaContent mediaContent = mediaContentRepository.findById(mediaContentId).orElseThrow(() -> new MediaContentNotFoundException("Media Content not found with id " + mediaContentId));
        Film film = filmRepository.findById(filmId).orElseThrow(() -> new FilmNotFoundException("Film not found with id " + filmId));

        mediaContent.setFilm(film);

        try (IsoFile isoFile = new IsoFile(new File("film/" + mediaContent.getFilm().getVideoUrl()))) {
            long duration = isoFile.getMovieBox().getMovieHeaderBox().getDuration() / isoFile.getMovieBox().getMovieHeaderBox().getTimescale();

            mediaContent.setDuration(duration);
        } catch (IOException e) {
            log.error("Failed to get duration of film: {}", e.getMessage());
        }

        MediaContent addedMediaContent = mediaContentRepository.save(mediaContent);

        log.info("Film added {} to Media Content {}", film.getId(), mediaContent.getId());

        return mediaContentMapper.toMediaContentResponse(addedMediaContent);
    }

    @Override
    @Caching(cacheable = {
            @Cacheable(value = "mediaContents", key = "#mediaContentId"),
            @Cacheable(value = "seasons", key = "#seasonId")
    })
    public MediaContentResponse addSeasonToMediaContent(long mediaContentId, long seasonId) {
        if (mediaContentRepository.existsByIdAndFilmIsNotNullOrSeasonsIsNotEmpty(mediaContentId)) {
            throw new MediaContentExistsVideoOrSeasonException("Episodes/Film already exists in Media Content");
        }

        MediaContent mediaContent = mediaContentRepository.findById(mediaContentId).orElseThrow(() -> new MediaContentNotFoundException("Media Content not found with id " + mediaContentId));

        Season season = seasonRepository.findById(seasonId).orElseThrow(() -> new SeasonNotFoundException("Season not found with id " + seasonId));

        mediaContent.getSeasons().add(season);

        long duration = 0;

        for (Episode episode : season.getEpisodes()) {
            try (IsoFile isoFile = new IsoFile(new File("episode/" + episode.getEpisodeUrl()))) {
                duration += isoFile.getMovieBox().getMovieHeaderBox().getDuration() / isoFile.getMovieBox().getMovieHeaderBox().getTimescale();

                mediaContent.setDuration(duration);
            } catch (IOException e) {
                log.error("Failed to get duration of episode: {}", e.getMessage());
            }
        }

        seasonRepository.save(season);
        MediaContent addedMediaContent = mediaContentRepository.save(mediaContent);

        log.info("Season added {} to Media Content {}", seasonId, mediaContentId);

        return mediaContentMapper.toMediaContentResponse(addedMediaContent);
    }

    @Override
    @Caching(cacheable = {
            @Cacheable(value = "mediaContents", key = "#mediaContentId"),
            @Cacheable(value = "films", key = "#filmId")
    })
    public void removeFilmFromMediaContent(long mediaContentId, long filmId) {
        MediaContent mediaContent = mediaContentRepository.findById(mediaContentId).orElseThrow(() -> new MediaContentNotFoundException("Media Content not found with id " + mediaContentId));

        Film film = filmRepository.findById(filmId).orElseThrow(() -> new FilmNotFoundException("Film not found with id " + filmId));

        if (!mediaContent.getFilm().equals(film)) {
            throw new FilmInMediaNotContainException("Film not found in Media Content");
        }

        log.info("Film deleted {} from Media Content {}", filmId, mediaContentId);

        mediaContent.setFilm(null);
        mediaContent.setDuration(0L);

        mediaContentRepository.save(mediaContent);
    }

    @Override
    @Caching(cacheable = {
            @Cacheable(value = "mediaContents", key = "#mediaContentId"),
            @Cacheable(value = "seasons", key = "#seasonId")
    })
    public void removeSeasonFromMediaContent(long mediaContentId, long seasonId) {
        MediaContent mediaContent = mediaContentRepository.findById(mediaContentId).orElseThrow(() -> new MediaContentNotFoundException("Media Content not found with id " + mediaContentId));

        Season season = seasonRepository.findById(seasonId).orElseThrow(() -> new SeasonNotFoundException("Season not found with id " + seasonId));

        if (!mediaContent.getSeasons().contains(season)) {
            throw new SeasonInMediaNotContainException("Seasons not found in Media Content");
        }

        mediaContent.getSeasons().remove(season);

        if (!mediaContent.getSeasons().isEmpty()) {
            long duration = 0;

            for (Episode episode : season.getEpisodes()) {
                try (IsoFile isoFile = new IsoFile(new File("episode/" + episode.getEpisodeUrl()))) {
                    duration += isoFile.getMovieBox().getMovieHeaderBox().getDuration() / isoFile.getMovieBox().getMovieHeaderBox().getTimescale();

                    mediaContent.setDuration(duration);
                } catch (IOException e) {
                    log.error("Failed to get duration of episode: {}", e.getMessage());
                }
            }
        } else {
            mediaContent.setDuration(0L);
        }

        mediaContentRepository.save(mediaContent);

        log.info("Season deleted {} from Media Content {}", seasonId, mediaContentId);
    }

    @Override
    @Cacheable(value = "mediaContents", key = "#id")
    public MediaContentResponse updateMediaContent(MediaContentRequest updateMediaContent, long id) {
        MediaContentResponse mediaContentResponse = findById(id);

        MediaContent mediaContent = mediaContentMapper.toMediaContent(mediaContentResponse);

        if (updateMediaContent.getTitle() != null) {
            mediaContent.setTitle(updateMediaContent.getTitle());
        } else if (updateMediaContent.getDescription() != null) {
            mediaContent.setDescription(updateMediaContent.getDescription());
        } else if (updateMediaContent.getReleaseDate() != null) {
            mediaContent.setReleaseDate(updateMediaContent.getReleaseDate());
        } else if (updateMediaContent.getTrailerUrl() != null) {
            mediaContent.setTrailerUrl(updateMediaContent.getTrailerUrl());
        } else if (updateMediaContent.getCompanyName() != null) {
            mediaContent.setCompanyName(updateMediaContent.getCompanyName());
        }

        MediaContent savedMediaContent = mediaContentRepository.save(mediaContent);

        return mediaContentMapper.toMediaContentResponse(savedMediaContent);
    }

    @Override
    public PaginatedResponse findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("releaseDate").descending());

        Page<MediaContent> pageMediaContent = mediaContentRepository.findAllByStatus(pageable, Status.PUBLIC);

        List<MediaContent> mediaContents = pageMediaContent.getContent();

        List<MediaContentResponse> mediaContentResponses = mediaContentMapper.toMediaContentResponse(mediaContents);

        return new PaginatedResponse(
                mediaContentResponses,
                pageMediaContent.getNumber(),
                pageMediaContent.getSize(),
                pageMediaContent.hasNext(),
                pageMediaContent.hasPrevious(),
                pageMediaContent.getTotalElements(),
                pageMediaContent.getTotalPages(),
                pageMediaContent.isFirst(),
                pageMediaContent.isLast()
        );
    }

    @Override
    public PaginatedResponse getMediaContentsBetweenStartReleaseDateAndEndReleaseDate(int page, int size, LocalDate startReleaseDate, LocalDate endReleaseDate) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("releaseDate").descending());

        Page<MediaContent> pageMediaContent = mediaContentRepository.findByReleaseDateBetweenAndStatus(pageable, startReleaseDate, endReleaseDate, Status.PUBLIC);

        List<MediaContent> mediaContents = pageMediaContent.getContent();

        List<MediaContentResponse> mediaContentResponses = mediaContentMapper.toMediaContentResponse(mediaContents);

        return new PaginatedResponse(
                mediaContentResponses,
                pageMediaContent.getNumber(),
                pageMediaContent.getSize(),
                pageMediaContent.hasNext(),
                pageMediaContent.hasPrevious(),
                pageMediaContent.getTotalElements(),
                pageMediaContent.getTotalPages(),
                pageMediaContent.isFirst(),
                pageMediaContent.isLast()
        );
    }

    @Override
    public MediaContentResponse addActorToMediaContent(long mediaContentId, long actorId) {
        Actor actor = actorService.findById(actorId);

        MediaContent mediaContent = mediaContentRepository.findById(mediaContentId).orElseThrow(() -> new MediaContentNotFoundException("Media Content not found with " + mediaContentId));

        if (mediaContent.getActors().contains(actor)) {
            throw new ActorInMediaContentExistException("Actor already exists in Media Content");
        }

        mediaContent.getActors().add(actor);

        MediaContent addedActorToMediaContent = mediaContentRepository.save(mediaContent);

        return mediaContentMapper.toMediaContentResponse(addedActorToMediaContent);
    }

    @Override
    public MediaContentResponse removeActorFromMediaContent(long mediaContentId, long actorId) {
        MediaContent mediaContent = mediaContentRepository.findById(mediaContentId).orElseThrow(() -> new MediaContentNotFoundException("Media Content not found with " + mediaContentId));

        Actor actor = actorService.findById(actorId);

        if (!mediaContent.getActors().contains(actor)) {
            throw new ActorInMediaContentNotExistException("Actor not exists in Media Content");
        }

        mediaContent.getActors().remove(actor);
        actor.getMediaContents().remove(mediaContent);

        MediaContent deletedActorToMediaContent = mediaContentRepository.save(mediaContent);

        return mediaContentMapper.toMediaContentResponse(deletedActorToMediaContent);
    }

    @Override
    public MediaContentResponse uploadAvatarById(long id, MultipartFile image) throws IOException {
        MediaContent mediaContentFound = mediaContentRepository.findById(id).orElseThrow(() -> new MediaContentNotFoundException("Media Content not found with " + id));

        String fileName = UUID.randomUUID().toString() + "_" + image.getOriginalFilename();

        Path path = Paths.get(DIRECTORY).resolve(fileName).normalize();

        Files.copy(image.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

        mediaContentFound.getCoverArts().add(fileName);

        MediaContent uploadedMedia = mediaContentRepository.save(mediaContentFound);

        return mediaContentMapper.toMediaContentResponse(uploadedMedia);
    }

    @Override
    public PaginatedResponse findByTitle(int page, int size, String title) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("releaseDate").descending());

        Page<MediaContent> pageMediaContents = mediaContentRepository.findByTitleContainingAndStatus(title, Status.PUBLIC, pageable);

        List<MediaContent> mediaContents = pageMediaContents.getContent();

        List<MediaContentResponse> mediaContentResponses = mediaContentMapper.toMediaContentResponse(mediaContents);

        return new PaginatedResponse(
                mediaContentResponses,
                page,
                size,
                pageMediaContents.hasNext(),
                pageMediaContents.hasPrevious(),
                pageMediaContents.getTotalElements(),
                pageMediaContents.getTotalPages(),
                pageMediaContents.isFirst(),
                pageMediaContents.isLast()
        );
    }
}
