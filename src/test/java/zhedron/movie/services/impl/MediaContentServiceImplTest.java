package zhedron.movie.services.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import zhedron.movie.dto.response.MediaContentResponse;
import zhedron.movie.dto.response.PaginatedResponse;
import zhedron.movie.entity.Film;
import zhedron.movie.entity.MediaContent;
import zhedron.movie.enums.Status;
import zhedron.movie.exceptions.FilmInMediaNotContainException;
import zhedron.movie.exceptions.MediaContentExistsVideoOrSeasonException;
import zhedron.movie.exceptions.MediaContentNotFoundException;
import zhedron.movie.mappers.MediaContentMapper;
import zhedron.movie.repository.FilmRepository;
import zhedron.movie.repository.MediaContentRepository;
import zhedron.movie.repository.SeasonRepository;
import zhedron.movie.services.UserService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaContentServiceImplTest {
    @Mock
    private MediaContentRepository mediaContentRepository;
    @Mock
    private FilmRepository filmRepository;
    @Mock
    private SeasonRepository seasonRepository;
    @Mock
    private MediaContentMapper mediaContentMapper;
    @Mock
    private UserService userService;
    @InjectMocks
    private MediaContentServiceImpl mediaContentService;

    @Test
    void uploadAvatarStoresCoverArtAndSavesMediaContent() throws Exception {
        MediaContent mediaContent = mediaContent(5L, Status.PRIVATE);
        mediaContent.setCoverArts(new ArrayList<>());
        MultipartFile image = new MockMultipartFile("image", "cover.png", "image/png", "image".getBytes());
        MediaContentResponse expectedResponse = response(5L, Status.PRIVATE);
        when(mediaContentRepository.findById(5L)).thenReturn(Optional.of(mediaContent));
        when(mediaContentRepository.save(mediaContent)).thenReturn(mediaContent);
        when(mediaContentMapper.toMediaContentResponse(mediaContent)).thenReturn(expectedResponse);

        MediaContentResponse response = mediaContentService.uploadAvatarById(5L, image);

        assertEquals(1, mediaContent.getCoverArts().size());
        assertTrue(mediaContent.getCoverArts().get(0).endsWith("_cover.png"));
        assertTrue(Files.exists(Path.of("cover_art", mediaContent.getCoverArts().get(0))));
        assertSame(expectedResponse, response);
        verify(mediaContentRepository).save(mediaContent);
        Files.deleteIfExists(Path.of("cover_art", mediaContent.getCoverArts().get(0)));
    }

    @Test
    void findByIdThrowsWhenMediaContentDoesNotExist() {
        when(mediaContentRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(MediaContentNotFoundException.class, () -> mediaContentService.findById(404L));
    }

    @Test
    void changeVisibilitySavesMediaContentWithRequestedStatus() {
        MediaContent mediaContent = mediaContent(5L, Status.PRIVATE);
        MediaContentResponse publicResponse = response(5L, Status.PUBLIC);
        when(mediaContentRepository.findById(5L)).thenReturn(Optional.of(mediaContent));
        when(mediaContentMapper.toMediaContentResponse(mediaContent)).thenReturn(publicResponse);
        when(mediaContentRepository.save(mediaContent)).thenReturn(mediaContent);

        MediaContentResponse response = mediaContentService.changeVisibility(Status.PUBLIC, 5L);

        assertEquals(Status.PUBLIC, mediaContent.getStatus());
        assertSame(publicResponse, response);
        verify(mediaContentRepository).save(mediaContent);
    }

    @Test
    void addFilmToMediaContentThrowsWhenMediaAlreadyHasVideoOrSeason() {
        when(mediaContentRepository.existsByIdAndFilmIsNotNullOrSeasonsIsNotEmpty(3L)).thenReturn(true);

        assertThrows(MediaContentExistsVideoOrSeasonException.class, () -> mediaContentService.addFilmToMediaContent(3L, 2L));

        verify(filmRepository, never()).findById(any());
    }

    @Test
    void deleteFilmFromMediaContentRejectsUnlinkedFilm() {
        Film attachedFilm = new Film();
        attachedFilm.setId(1L);
        Film requestedFilm = new Film();
        requestedFilm.setId(2L);
        MediaContent mediaContent = mediaContent(7L, Status.PRIVATE);
        mediaContent.setFilm(attachedFilm);
        when(mediaContentRepository.findById(7L)).thenReturn(Optional.of(mediaContent));
        when(filmRepository.findById(2L)).thenReturn(Optional.of(requestedFilm));

        assertThrows(FilmInMediaNotContainException.class, () -> mediaContentService.removeFilmFromMediaContent(7L, 2L));
    }

    @Test
    void findAllReturnsOnlyPublicItemsButKeepsPageMetadata() {
        MediaContent publicMedia = mediaContent(1L, Status.PUBLIC);
        MediaContent privateMedia = mediaContent(2L, Status.PRIVATE);
        MediaContentResponse publicResponse = response(1L, Status.PUBLIC);
        when(mediaContentRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(publicMedia, privateMedia)));
        when(mediaContentMapper.toMediaContentResponse(List.of(publicMedia))).thenReturn(List.of(publicResponse));

        PaginatedResponse response = mediaContentService.findAll(0, 10);

        assertEquals(List.of(publicResponse), response.mediaContentResponses());
        assertEquals(0, response.page());
        assertEquals(2, response.totalElements());
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(mediaContentRepository).findAll(pageableCaptor.capture());
        assertEquals("releaseDate: DESC", pageableCaptor.getValue().getSort().toString());
    }

    private static MediaContent mediaContent(long id, Status status) {
        MediaContent mediaContent = new MediaContent();
        mediaContent.setId(id);
        mediaContent.setTitle("Title " + id);
        mediaContent.setDescription("Description " + id);
        mediaContent.setStatus(status);
        mediaContent.setSeasons(new ArrayList<>());
        return mediaContent;
    }

    private static MediaContentResponse response(long id, Status status) {
        return new MediaContentResponse(id, "Title " + id, "Description " + id, null, 0, null, null, List.of(), status, null, null, List.of(), List.of(), List.of());
    }
}
