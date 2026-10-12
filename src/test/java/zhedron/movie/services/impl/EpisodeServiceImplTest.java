package zhedron.movie.services.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import zhedron.movie.dto.response.EpisodeResponse;
import zhedron.movie.dto.response.request.EpisodeCreateRequest;
import zhedron.movie.entity.Episode;
import zhedron.movie.entity.Subtitle;
import zhedron.movie.exceptions.EpisodeNotFoundException;
import zhedron.movie.mappers.EpisodeMapper;
import zhedron.movie.repository.EpisodeRepository;
import zhedron.movie.repository.SubtitleRepository;
import zhedron.movie.services.SubtitleService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EpisodeServiceImplTest {
    @Mock
    private EpisodeRepository episodeRepository;
    @Mock
    private SubtitleRepository subtitleRepository;
    @Mock
    private EpisodeMapper episodeMapper;
    @Mock
    private SubtitleService subtitleService;
    @InjectMocks
    private EpisodeServiceImpl episodeService;

    private final List<Path> filesToDelete = new ArrayList<>();

    @AfterEach
    void cleanUpFiles() throws IOException {
        for (Path file : filesToDelete) {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void uploadEpisodeStoresFileMetadataAndReturnsResponse() throws IOException {
        MockMultipartFile video = new MockMultipartFile("video", "episode.mp4", "video/mp4", "video".getBytes());
        EpisodeCreateRequest request = new EpisodeCreateRequest();
        request.setEpisodeNumber(4);
        Episode savedEpisode = new Episode();
        savedEpisode.setId(10L);
        savedEpisode.setEpisodeUrl("saved-episode.mp4");
        savedEpisode.setEpisodeNumber(4);
        savedEpisode.setContentType("video/mp4");
        EpisodeResponse expectedResponse = new EpisodeResponse(10L, "saved-episode.mp4", 4, "video/mp4", List.of());
        when(episodeRepository.save(any(Episode.class))).thenReturn(savedEpisode);
        when(episodeMapper.toEpisodeResponse(savedEpisode)).thenReturn(expectedResponse);

        EpisodeResponse response = episodeService.uploadEpisode(video, request);

        ArgumentCaptor<Episode> episodeCaptor = ArgumentCaptor.forClass(Episode.class);
        verify(episodeRepository).save(episodeCaptor.capture());
        Episode episodeToSave = episodeCaptor.getValue();
        filesToDelete.add(Path.of("episode").resolve(episodeToSave.getEpisodeUrl()));
        assertTrue(episodeToSave.getEpisodeUrl().endsWith("_episode.mp4"));
        assertEquals(4, episodeToSave.getEpisodeNumber());
        assertEquals("video/mp4", episodeToSave.getContentType());
        assertTrue(Files.exists(Path.of("episode").resolve(episodeToSave.getEpisodeUrl())));
        assertEquals(expectedResponse, response);
    }

    @Test
    void findByIdReturnsMappedEpisode() {
        Episode episode = new Episode();
        episode.setId(8L);
        EpisodeResponse expectedResponse = new EpisodeResponse(8L, "episode.mp4", 2, "video/mp4", List.of());
        when(episodeRepository.findById(8L)).thenReturn(Optional.of(episode));
        when(episodeMapper.toEpisodeResponse(episode)).thenReturn(expectedResponse);

        assertEquals(expectedResponse, episodeService.findById(8L));
    }

    @Test
    void findByIdThrowsWhenEpisodeIsMissing() {
        when(episodeRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(EpisodeNotFoundException.class, () -> episodeService.findById(404L));
    }

    @Test
    void deleteByIdDeletesRepositoryEntityAndFile() throws IOException {
        Files.createDirectories(Path.of("episode"));
        Path videoPath = Path.of("episode").resolve("delete-me.mp4");
        Files.writeString(videoPath, "video");
        Episode episode = new Episode();
        episode.setId(12L);
        episode.setEpisodeUrl("delete-me.mp4");
        when(episodeRepository.findById(12L)).thenReturn(Optional.of(episode));

        episodeService.deleteById(12L);

        verify(episodeRepository).deleteById(12L);
        assertFalse(Files.exists(videoPath));
    }

    @Test
    void addSubtitleLinksAndReleasesSubtitle() {
        Episode episode = new Episode();
        episode.setId(7L);
        episode.setSubtitles(new ArrayList<>());
        Subtitle subtitle = new Subtitle();
        subtitle.setId(3);
        EpisodeResponse expected = new EpisodeResponse(7L, "episode.mp4", 2, "video/mp4", List.of());
        when(episodeRepository.findById(7L)).thenReturn(Optional.of(episode));
        when(subtitleService.findById(3L)).thenReturn(subtitle);
        when(episodeRepository.save(episode)).thenReturn(episode);
        when(episodeMapper.toEpisodeResponse(episode)).thenReturn(expected);

        assertEquals(expected, episodeService.addSubtitleToEpisode(7L, 3L));

        assertTrue(subtitle.isReleased());
        assertEquals(episode, subtitle.getEpisode());
        assertTrue(episode.getSubtitles().contains(subtitle));
        verify(subtitleRepository).save(subtitle);
    }

    @Test
    void removeSubtitleRejectsMissingAssociation() {
        Episode episode = new Episode();
        episode.setId(7L);
        episode.setSubtitles(new ArrayList<>());
        when(episodeRepository.findById(7L)).thenReturn(Optional.of(episode));
        when(subtitleService.findById(3L)).thenReturn(new Subtitle());

        assertThrows(zhedron.movie.exceptions.SubtitleNotFoundException.class,
                () -> episodeService.removeSubtitleFromEpisode(7L, 3L));
    }
}
