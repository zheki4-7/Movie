package zhedron.movie.services.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import zhedron.movie.dto.response.SubtitleResponse;
import zhedron.movie.entity.Subtitle;
import zhedron.movie.exceptions.SrtFileParsingException;
import zhedron.movie.exceptions.SubtitleNotFoundException;
import zhedron.movie.mappers.SubtitleMapper;
import zhedron.movie.repository.SubtitleRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubtitleServiceImplTest {
    @Mock
    private SubtitleRepository subtitleRepository;
    @Mock
    private SubtitleMapper subtitleMapper;
    @InjectMocks
    private SubtitleServiceImpl subtitleService;

    private Path uploadedFile;

    @AfterEach
    void cleanUp() throws IOException {
        if (uploadedFile != null) {
            Files.deleteIfExists(uploadedFile);
        }
    }

    @Test
    void uploadFileStoresMetadataAndReturnsResponse() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "test-subtitle.srt", "application/x-subrip", "content".getBytes());
        Subtitle saved = new Subtitle();
        saved.setId(4);
        saved.setSrtName("test-subtitle.srt");
        SubtitleResponse expected = new SubtitleResponse(4, "test-subtitle.srt", false, 0, 0);
        when(subtitleRepository.save(any(Subtitle.class))).thenReturn(saved);
        when(subtitleMapper.toSubtitleResponse(saved)).thenReturn(expected);

        assertEquals(expected, subtitleService.uploadFile(file));
        uploadedFile = Path.of("subtitles").resolve("test-subtitle.srt");
        verify(subtitleRepository).save(any(Subtitle.class));
        assertEquals("test-subtitle.srt", saved.getSrtName());
        assertFalse(saved.isReleased());
        org.junit.jupiter.api.Assertions.assertTrue(Files.exists(uploadedFile));
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(subtitleRepository.findById(404L)).thenReturn(Optional.empty());

        SubtitleNotFoundException exception = assertThrows(SubtitleNotFoundException.class,
                () -> subtitleService.findById(404L));

        assertEquals("Subtitle not found with 404", exception.getMessage());
    }

    @Test
    void deleteThrowsWhenMissing() {
        when(subtitleRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(SubtitleNotFoundException.class, () -> subtitleService.deleteSubtitleById(404L));
    }

    @Test
    void deleteRemovesSrtFileAndRepositoryEntity() throws IOException {
        Files.createDirectories(Path.of("subtitles"));
        Path file = Path.of("subtitles").resolve("delete-test.srt");
        Files.writeString(file, "subtitle");
        uploadedFile = file;

        Subtitle subtitle = new Subtitle();
        subtitle.setId(8);
        subtitle.setSrtName("delete-test.srt");
        when(subtitleRepository.findById(8L)).thenReturn(Optional.of(subtitle));

        subtitleService.deleteSubtitleById(8L);

        assertFalse(Files.exists(file));
        verify(subtitleRepository).deleteById(8L);
    }

    @Test
    void parsesSrtCues() throws IOException {
        Files.createDirectories(Path.of("subtitles"));
        Path file = Path.of("subtitles").resolve("parse-test.srt");
        Files.writeString(file, "1\r\n00:00:00,000 --> 00:00:01,000\r\nHello\r\nworld\r\n\r\n2\r\n00:00:02,000 --> 00:00:03,000\r\nSecond");
        uploadedFile = file;

        List<java.util.Map<String, Object>> cues = subtitleService.getDataFromSrtFile(Path.of("parse-test.srt"));

        assertEquals(2, cues.size());
        assertEquals("Hello world", cues.get(0).get("text"));
        assertEquals("00:00:00,000", cues.get(0).get("startTime"));
        assertEquals(2, cues.get(1).get("number"));
    }

    @Test
    void throwsSrtFileParsingExceptionForMalformedSrtBlock() throws IOException {
        Files.createDirectories(Path.of("subtitles"));
        Path file = Path.of("subtitles").resolve("malformed-test.srt");
        Files.writeString(file, "1\r\n00:00:00,000 --> 00:00:01,000");
        uploadedFile = file;

        SrtFileParsingException exception = assertThrows(SrtFileParsingException.class,
                () -> subtitleService.getDataFromSrtFile(Path.of("malformed-test.srt")));

        assertEquals("Failed to read file", exception.getMessage());
    }
}
