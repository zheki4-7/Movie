package zhedron.movie.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import zhedron.movie.dto.response.EpisodeResponse;
import zhedron.movie.dto.response.request.EpisodeCreateRequest;
import zhedron.movie.entity.Episode;
import zhedron.movie.entity.Subtitle;
import zhedron.movie.exceptions.EpisodeNotFoundException;
import zhedron.movie.exceptions.SubtitleExistInEpisodeException;
import zhedron.movie.exceptions.SubtitleNotFoundException;
import zhedron.movie.mappers.EpisodeMapper;
import zhedron.movie.repository.EpisodeRepository;
import zhedron.movie.repository.SubtitleRepository;
import zhedron.movie.services.EpisodeService;
import zhedron.movie.services.SubtitleService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class EpisodeServiceImpl implements EpisodeService {
    private final EpisodeRepository episodeRepository;
    private final SubtitleRepository subtitleRepository;

    private final EpisodeMapper episodeMapper;

    private final SubtitleService subtitleService;

    private final String DIRECTORY = "episode/";

    public EpisodeServiceImpl(EpisodeRepository episodeRepository, SubtitleRepository subtitleRepository, EpisodeMapper episodeMapper, SubtitleService subtitleService) {
        this.episodeRepository = episodeRepository;
        this.subtitleRepository = subtitleRepository;
        this.episodeMapper = episodeMapper;
        this.subtitleService = subtitleService;
    }

    @Override
    @CachePut(value = "episodes", key = "#result.id()")
    public EpisodeResponse uploadEpisode(MultipartFile video, EpisodeCreateRequest episodeCreateRequest) throws IOException {
        Episode episode = new Episode();

        Path path = Paths.get(DIRECTORY);

        if (Files.notExists(path)) {
            Files.createDirectories(path);
        }

        String fileName = UUID.randomUUID().toString() + "_" + video.getOriginalFilename();

        Path videoFile = Paths.get(DIRECTORY).resolve(fileName).normalize();

        Files.copy(video.getInputStream(), videoFile, StandardCopyOption.REPLACE_EXISTING);

        episode.setEpisodeUrl(fileName);
        episode.setEpisodeNumber(episodeCreateRequest.getEpisodeNumber());
        episode.setContentType(video.getContentType());

        Episode savedEpisode = episodeRepository.save(episode);

        return episodeMapper.toEpisodeResponse(savedEpisode);
    }

    @Override
    @Cacheable(value = "episodes", key = "#id")
    public EpisodeResponse findById(long id) {
        Episode episode = episodeRepository.findById(id).orElseThrow(() -> new EpisodeNotFoundException("Episode not found with " + id));

        return episodeMapper.toEpisodeResponse(episode);
    }

    @Override
    @CacheEvict(value = "episodes", key = "#id")
    public void deleteById(long id) {
        Episode episode = episodeRepository.findById(id).orElseThrow(() -> new EpisodeNotFoundException("Episode not found with " + id));
        episodeRepository.deleteById(id);

        try {
            Path path = Paths.get(DIRECTORY).resolve(episode.getEpisodeUrl()).normalize();

            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.error("Failed to delete file: {}", e.getMessage());
        }
    }

    @Override
    @Caching(
            cacheable = {
                    @Cacheable(
                            value = "subtitles", key = "#subtitleId"
                    ),
                    @Cacheable(
                            value = "episodes", key = "#episodeId"
                    )
            }
    )
    public EpisodeResponse addSubtitleToEpisode(long episodeId, long subtitleId) {
        Episode episode = episodeRepository.findById(episodeId).orElseThrow(() -> new EpisodeNotFoundException("Episode not found with " + episodeId));

        Subtitle subtitle = subtitleService.findById(subtitleId);

        if (episode.getSubtitles().contains(subtitle)) {
            throw new SubtitleExistInEpisodeException("Subtitle exists in Episode");
        }

        subtitle.setReleased(true);
        subtitle.setEpisode(episode);

        subtitleRepository.save(subtitle);

        episode.getSubtitles().add(subtitle);

        Episode addedSubtitle = episodeRepository.save(episode);

        return episodeMapper.toEpisodeResponse(addedSubtitle);
    }

    @Override
    @Caching(
            cacheable = {
                    @Cacheable(
                            value = "subtitles", key = "#subtitleId"
                    ),
                    @Cacheable(
                            value = "episodes", key = "#episodeId"
                    )
            }
    )
    public EpisodeResponse removeSubtitleFromEpisode(long episodeId, long subtitleId) {
        Episode episode = episodeRepository.findById(episodeId).orElseThrow(() -> new EpisodeNotFoundException("Episode not found with " + episodeId));

        Subtitle subtitle = subtitleService.findById(subtitleId);

        if (!episode.getSubtitles().contains(subtitle)) {
            throw new SubtitleNotFoundException("Episode subtitle not found in Episode");
        }

        episode.getSubtitles().remove(subtitle);

        Episode removedSubtitle = episodeRepository.save(episode);

        return episodeMapper.toEpisodeResponse(removedSubtitle);
    }
}