package zhedron.movie.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import zhedron.movie.config.SecurityConfig;
import zhedron.movie.dto.response.SubtitleResponse;
import zhedron.movie.entity.Subtitle;
import zhedron.movie.exceptions.SrtFileParsingException;
import zhedron.movie.mappers.SubtitleMapper;
import zhedron.movie.repository.SubtitleRepository;
import zhedron.movie.repository.UserRepository;
import zhedron.movie.services.SubtitleService;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubtitleController.class)
@Import({SecurityConfig.class, ControllerSecurityTestConfig.class})
class SubtitleControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private SubtitleService subtitleService;
    @MockitoBean
    private SubtitleRepository subtitleRepository;
    @MockitoBean
    private SubtitleMapper subtitleMapper;
    @MockitoBean
    private UserRepository userRepository;

    @Test
    void getSubtitleReturnsMetadata() throws Exception {
        Subtitle subtitle = new Subtitle();
        SubtitleResponse response = new SubtitleResponse(3, "english.srt", true, 6, 0);
        when(subtitleService.findById(3)).thenReturn(subtitle);
        when(subtitleMapper.toSubtitleResponse(subtitle)).thenReturn(response);

        mockMvc.perform(get("/api/subtitle/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.filmId").value(6));
    }

    @Test
    void getSubtitleReturnsNotFoundWhenServiceCannotFindSubtitle() throws Exception {
        when(subtitleService.findById(404))
                .thenThrow(new zhedron.movie.exceptions.SubtitleNotFoundException("Subtitle not found with 404"));

        mockMvc.perform(get("/api/subtitle/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Subtitle not found with 404"));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void uploadSrtReturnsCreatedSubtitle() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "english.srt", "application/x-subrip", "1\n00:00:00,000 --> 00:00:01,000\nHello".getBytes());
        when(subtitleService.uploadFile(any())).thenReturn(new SubtitleResponse(3, "english.srt", false, 0, 0));

        mockMvc.perform(multipart("/api/subtitle/upload").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void uploadRejectsNonSrtFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "captions.txt", MediaType.TEXT_PLAIN_VALUE, "captions".getBytes());

        mockMvc.perform(multipart("/api/subtitle/upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Upload srt file"));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void deleteSubtitleReturnsMessage() throws Exception {
        mockMvc.perform(delete("/api/subtitle/delete/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Subtitle deleted successfully"));

        verify(subtitleService).deleteSubtitleById(3L);
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void deleteSubtitleReturnsNotFoundWhenServiceCannotFindSubtitle() throws Exception {
        doThrow(new zhedron.movie.exceptions.SubtitleNotFoundException("Subtitle not found with 3"))
                .when(subtitleService).deleteSubtitleById(3L);

        mockMvc.perform(delete("/api/subtitle/delete/3"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Subtitle not found with 3"));
    }

    @Test
    void uploadSubtitleRejectsAnonymousUser() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "english.srt", "application/x-subrip", "content".getBytes());

        mockMvc.perform(multipart("/api/subtitle/upload").file(file))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "USER")
    void uploadSubtitleRejectsNonAdminUser() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "english.srt", "application/x-subrip", "content".getBytes());

        mockMvc.perform(multipart("/api/subtitle/upload").file(file))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteSubtitleRejectsAnonymousUser() throws Exception {
        mockMvc.perform(delete("/api/subtitle/delete/3"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "USER")
    void deleteSubtitleRejectsNonAdminUser() throws Exception {
        mockMvc.perform(delete("/api/subtitle/delete/3"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getFilmSubtitleDataReturnsParsedCues() throws Exception {
        Subtitle subtitle = new Subtitle();
        subtitle.setSrtName("english.srt");
        when(subtitleRepository.findByFilmId(6)).thenReturn(subtitle);
        when(subtitleService.getDataFromSrtFile(any())).thenReturn(List.of(Map.of("number", 1)));

        mockMvc.perform(get("/api/subtitle/datasrt/film/6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].number").value(1));
    }

    @Test
    void getFilmSubtitleDataReturnsSrtParsingError() throws Exception {
        Subtitle subtitle = new Subtitle();
        subtitle.setSrtName("malformed.srt");
        when(subtitleRepository.findByFilmId(6)).thenReturn(subtitle);
        when(subtitleService.getDataFromSrtFile(any()))
                .thenThrow(new SrtFileParsingException("Failed to read file"));

        mockMvc.perform(get("/api/subtitle/datasrt/film/6"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Failed to read file"));
    }
}
