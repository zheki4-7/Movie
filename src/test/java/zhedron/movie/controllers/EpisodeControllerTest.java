package zhedron.movie.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import zhedron.movie.dto.response.EpisodeResponse;
import zhedron.movie.dto.response.request.EpisodeCreateRequest;
import zhedron.movie.repository.UserRepository;
import zhedron.movie.services.EpisodeService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EpisodeController.class)
@Import({SecurityConfig.class, ControllerSecurityTestConfig.class})
class EpisodeControllerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private EpisodeService episodeService;
    @MockitoBean
    private UserRepository userRepository;

    @Test
    @WithMockUser(authorities = "ADMIN")
    void uploadEpisodeAcceptsMp4AndRequestPart() throws Exception {
        EpisodeCreateRequest request = new EpisodeCreateRequest();
        request.setEpisodeNumber(2);
        MockMultipartFile video = new MockMultipartFile("video", "episode.mp4", "video/mp4", "video".getBytes());
        MockMultipartFile requestPart = new MockMultipartFile(
                "episodeCreateRequest", "", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(request));
        when(episodeService.uploadEpisode(any(), any())).thenReturn(new EpisodeResponse(7L, "episode.mp4", 2, "video/mp4", List.of()));

        mockMvc.perform(multipart("/api/episode/upload").file(video).file(requestPart))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.episodeNumber").value(2));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void uploadEpisodeRejectsEmptyVideo() throws Exception {
        EpisodeCreateRequest request = new EpisodeCreateRequest();
        request.setEpisodeNumber(2);
        MockMultipartFile video = new MockMultipartFile("video", "episode.mp4", "video/mp4", new byte[0]);
        MockMultipartFile requestPart = new MockMultipartFile(
                "episodeCreateRequest", "", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(request));

        mockMvc.perform(multipart("/api/episode/upload").file(video).file(requestPart))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Upload video file"));
    }

    @Test
    void uploadEpisodeRejectsAnonymousUser() throws Exception {
        EpisodeCreateRequest request = new EpisodeCreateRequest();
        request.setEpisodeNumber(2);
        MockMultipartFile video = new MockMultipartFile("video", "episode.mp4", "video/mp4", "video".getBytes());
        MockMultipartFile requestPart = new MockMultipartFile(
                "episodeCreateRequest", "", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(request));

        mockMvc.perform(multipart("/api/episode/upload").file(video).file(requestPart))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "USER")
    void uploadEpisodeRejectsNonAdminUser() throws Exception {
        EpisodeCreateRequest request = new EpisodeCreateRequest();
        request.setEpisodeNumber(2);
        MockMultipartFile video = new MockMultipartFile("video", "episode.mp4", "video/mp4", "video".getBytes());
        MockMultipartFile requestPart = new MockMultipartFile(
                "episodeCreateRequest", "", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(request));

        mockMvc.perform(multipart("/api/episode/upload").file(video).file(requestPart))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void deleteEpisodeReturnsMessage() throws Exception {
        mockMvc.perform(delete("/api/episode/delete/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Episode deleted successfully"));

        verify(episodeService).deleteById(7L);
    }

    @Test
    void deleteEpisodeRejectsAnonymousUser() throws Exception {
        mockMvc.perform(delete("/api/episode/delete/7"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "USER")
    void deleteEpisodeRejectsNonAdminUser() throws Exception {
        mockMvc.perform(delete("/api/episode/delete/7"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void addSubtitleToEpisodeReturnsUpdatedEpisode() throws Exception {
        when(episodeService.addSubtitleToEpisode(7L, 3L))
                .thenReturn(new EpisodeResponse(7L, "episode.mp4", 2, "video/mp4", List.of()));

        mockMvc.perform(post("/api/episode/add/subtitle/3/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));

        verify(episodeService).addSubtitleToEpisode(7L, 3L);
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void removeSubtitleFromEpisodeReturnsUpdatedEpisode() throws Exception {
        when(episodeService.removeSubtitleFromEpisode(7L, 3L))
                .thenReturn(new EpisodeResponse(7L, "episode.mp4", 2, "video/mp4", List.of()));

        mockMvc.perform(delete("/api/episode/remove/subtitle/3/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));

        verify(episodeService).removeSubtitleFromEpisode(7L, 3L);
    }

    @Test
    void addSubtitleToEpisodeRejectsAnonymousUser() throws Exception {
        mockMvc.perform(post("/api/episode/add/subtitle/3/7"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "USER")
    void addSubtitleToEpisodeRejectsNonAdminUser() throws Exception {
        mockMvc.perform(post("/api/episode/add/subtitle/3/7"))
                .andExpect(status().isForbidden());
    }

    @Test
    void removeSubtitleFromEpisodeRejectsAnonymousUser() throws Exception {
        mockMvc.perform(delete("/api/episode/remove/subtitle/3/7"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "USER")
    void removeSubtitleFromEpisodeRejectsNonAdminUser() throws Exception {
        mockMvc.perform(delete("/api/episode/remove/subtitle/3/7"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void uploadEpisodeRejectsMissingVideo() throws Exception {
        EpisodeCreateRequest request = new EpisodeCreateRequest();
        request.setEpisodeNumber(2);
        MockMultipartFile requestPart = new MockMultipartFile(
                "episodeCreateRequest", "", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(request));

        mockMvc.perform(multipart("/api/episode/upload").file(requestPart))
                .andExpect(status().isBadRequest());
    }
}
