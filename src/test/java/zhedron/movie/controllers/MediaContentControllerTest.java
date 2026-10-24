package zhedron.movie.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
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
import zhedron.movie.dto.response.MediaContentResponse;
import zhedron.movie.dto.response.PaginatedResponse;
import zhedron.movie.dto.response.request.MediaContentRequest;
import zhedron.movie.enums.Status;
import zhedron.movie.repository.UserRepository;
import zhedron.movie.services.MediaContentService;

import java.time.LocalDate;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MediaContentController.class)
@Import({SecurityConfig.class, ControllerSecurityTestConfig.class})
class MediaContentControllerTest {
    private static final Path STREAM_IMAGE = Path.of("cover_art", "media-content-controller-stream.png");
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private MediaContentService mediaContentService;
    @MockitoBean
    private UserRepository userRepository;

    @AfterEach
    void cleanStreamImage() throws IOException {
        Files.deleteIfExists(STREAM_IMAGE);
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void createMediaContentAcceptsMultipartRequest() throws Exception {
        MediaContentRequest request = mediaContentRequest();
        MockMultipartFile requestPart = new MockMultipartFile(
                "mediaContentRequest", "", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(request));
        MockMultipartFile image = new MockMultipartFile("images", "cover.jpg", "image/jpeg", "image".getBytes());
        when(mediaContentService.createMediaContent(any(MediaContentRequest.class), any()))
                .thenReturn(response(8L, Status.PRIVATE));

        mockMvc.perform(multipart("/api/mediacontent/create").file(requestPart).file(image))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(8))
                .andExpect(jsonPath("$.status").value("PRIVATE"));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void createMediaContentRejectsEmptyImage() throws Exception {
        MediaContentRequest request = mediaContentRequest();
        MockMultipartFile requestPart = new MockMultipartFile(
                "mediaContentRequest", "", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(request));
        MockMultipartFile image = new MockMultipartFile("images", "cover.jpg", "image/jpeg", new byte[0]);

        mockMvc.perform(multipart("/api/mediacontent/create").file(requestPart).file(image))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Upload image file"));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void createMediaContentReturnsValidationMessageForBlankTitle() throws Exception {
        MediaContentRequest request = mediaContentRequest();
        request.setTitle("");
        MockMultipartFile requestPart = new MockMultipartFile(
                "mediaContentRequest", "", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(request));
        MockMultipartFile image = new MockMultipartFile("images", "cover.jpg", "image/jpeg", "image".getBytes());

        mockMvc.perform(multipart("/api/mediacontent/create").file(requestPart).file(image))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Write title, title must not be empty"));

        verify(mediaContentService, never()).createMediaContent(any(MediaContentRequest.class), any());
    }

    @Test
    @WithMockUser(authorities = "USER")
    void getMediaContentByIdReturnsServiceResponse() throws Exception {
        when(mediaContentService.findById(8L)).thenReturn(response(8L, Status.PUBLIC));

        mockMvc.perform(get("/api/mediacontent/8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(8))
                .andExpect(jsonPath("$.status").value("PUBLIC"));
    }

    @Test
    void getMediaContentByIdRejectsAnonymousUser() throws Exception {
        mockMvc.perform(get("/api/mediacontent/8"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void changeStatusReadsStatusFromBody() throws Exception {
        when(mediaContentService.changeVisibility(Status.PUBLIC, 8L)).thenReturn(response(8L, Status.PUBLIC));

        mockMvc.perform(put("/api/mediacontent/change-status/8")
                        .param("status", "PUBLIC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLIC"));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void getAllMediaContentsUsesPageParameters() throws Exception {
        PaginatedResponse page = new PaginatedResponse(List.of(response(8L, Status.PUBLIC)), 1, 5, false, true, 1, 1, false, true);
        when(mediaContentService.findAll(1, 5)).thenReturn(page);

        mockMvc.perform(get("/api/mediacontent").param("page", "1").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.mediaContentResponses[0].id").value(8));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void deleteMediaContentReturnsMessage() throws Exception {
        mockMvc.perform(delete("/api/mediacontent/delete/8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Media Content has been deleted"));

        verify(mediaContentService).deleteById(8L);
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void updateMediaContentReturnsValidationMessageForBlankTitle() throws Exception {
        MediaContentRequest request = mediaContentRequest();
        request.setTitle("");

        mockMvc.perform(put("/api/mediacontent/update/8")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Write title, title must not be empty"));

        verify(mediaContentService, never()).updateMediaContent(any(MediaContentRequest.class), eq(8L));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void addFilmToMediaContentUsesRequestParameters() throws Exception {
        when(mediaContentService.addFilmToMediaContent(8L, 2L)).thenReturn(response(8L, Status.PRIVATE));

        mockMvc.perform(post("/api/mediacontent/film")
                        .param("mediaContentId", "8")
                        .param("filmId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(8));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void addActorToMediaContentUsesPathVariables() throws Exception {
        when(mediaContentService.addActorToMediaContent(8L, 5L)).thenReturn(response(8L, Status.PRIVATE));

        mockMvc.perform(post("/api/mediacontent/actor/8/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(8));

        verify(mediaContentService).addActorToMediaContent(8L, 5L);
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void removeActorFromMediaContentUsesPathVariables() throws Exception {
        when(mediaContentService.removeActorFromMediaContent(8L, 5L)).thenReturn(response(8L, Status.PRIVATE));

        mockMvc.perform(delete("/api/mediacontent/delete/actor/8/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(8));

        verify(mediaContentService).removeActorFromMediaContent(8L, 5L);
    }

    @Test
    void streamPhotoMediaContentUsesRequestedPhotoContentType() throws Exception {
        Files.write(STREAM_IMAGE, "image".getBytes());
        MediaContentResponse mediaContent = new MediaContentResponse(
                8L, "Title", "Description", LocalDate.of(2026, 1, 1), 0,
                List.of(STREAM_IMAGE.getFileName().toString()), null, List.of(), Status.PUBLIC,
                null, "Studio", List.of(), List.of(MediaType.IMAGE_PNG_VALUE), List.of());
        when(mediaContentService.findById(8L)).thenReturn(mediaContent);

        mockMvc.perform(get("/api/mediacontent/stream/photo/8").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
    }

    private static MediaContentRequest mediaContentRequest() {
        MediaContentRequest request = new MediaContentRequest();
        request.setTitle("Title");
        request.setDescription("Description");
        request.setReleaseDate(LocalDate.of(2026, 1, 1));
        request.setCompanyName("Studio");
        request.setTrailerUrl("https://example.com/trailer");
        return request;
    }

    private static MediaContentResponse response(long id, Status status) {
        return new MediaContentResponse(id, "Title", "Description", LocalDate.of(2026, 1, 1), 0, List.of(), null, List.of(), status, null, "Studio", List.of(), List.of(), List.of());
    }
}
