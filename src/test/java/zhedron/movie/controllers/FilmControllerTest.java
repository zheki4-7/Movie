package zhedron.movie.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import zhedron.movie.config.SecurityConfig;
import zhedron.movie.dto.response.FilmResponse;
import zhedron.movie.repository.UserRepository;
import zhedron.movie.services.FilmService;
import zhedron.movie.services.SubtitleService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FilmController.class)
@Import({SecurityConfig.class, ControllerSecurityTestConfig.class})
class FilmControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private FilmService filmService;
    @MockitoBean
    private SubtitleService subtitleService;
    @MockitoBean
    private UserRepository userRepository;

    @Test
    @WithMockUser(authorities = "ADMIN")
    void uploadFilmAcceptsMp4() throws Exception {
        MockMultipartFile video = new MockMultipartFile("video", "film.mp4", "video/mp4", "video".getBytes());
        when(filmService.uploadFilm(any())).thenReturn(new FilmResponse(6L, "film.mp4", "video/mp4", List.of()));

        mockMvc.perform(multipart("/api/film/upload").file(video))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(6))
                .andExpect(jsonPath("$.contentType").value("video/mp4"));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void uploadFilmRejectsNonMp4() throws Exception {
        MockMultipartFile video = new MockMultipartFile("video", "film.mov", "video/quicktime", "video".getBytes());

        mockMvc.perform(multipart("/api/film/upload").file(video))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Upload video file with mp4 format"));
    }

    @Test
    void uploadFilmRejectsAnonymousUser() throws Exception {
        MockMultipartFile video = new MockMultipartFile("video", "film.mp4", "video/mp4", "video".getBytes());

        mockMvc.perform(multipart("/api/film/upload").file(video))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "USER")
    void uploadFilmRejectsNonAdminUser() throws Exception {
        MockMultipartFile video = new MockMultipartFile("video", "film.mp4", "video/mp4", "video".getBytes());

        mockMvc.perform(multipart("/api/film/upload").file(video))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void deleteFilmReturnsMessage() throws Exception {
        mockMvc.perform(delete("/api/film/delete/6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Film deleted successfully"));

        verify(filmService).deleteById(6L);
    }

    @Test
    void deleteFilmRejectsAnonymousUser() throws Exception {
        mockMvc.perform(delete("/api/film/delete/6"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "USER")
    void deleteFilmRejectsNonAdminUser() throws Exception {
        mockMvc.perform(delete("/api/film/delete/6"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void addSubtitleToFilmReturnsUpdatedFilm() throws Exception {
        FilmResponse response = new FilmResponse(6L, "film.mp4", "video/mp4", List.of());
        when(filmService.addSubtitleToFilm(3L, 6L)).thenReturn(response);

        mockMvc.perform(post("/api/film/add/subtitle/3/6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(6));

        verify(filmService).addSubtitleToFilm(3L, 6L);
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void removeSubtitleFromFilmReturnsUpdatedFilm() throws Exception {
        when(filmService.removeSubtitleFromFilm(3L, 6L))
                .thenReturn(new FilmResponse(6L, "film.mp4", "video/mp4", List.of()));

        mockMvc.perform(delete("/api/film/remove/subtitle/3/6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(6));

        verify(filmService).removeSubtitleFromFilm(3L, 6L);
    }

    @Test
    void addSubtitleToFilmRejectsAnonymousUser() throws Exception {
        mockMvc.perform(post("/api/film/add/subtitle/3/6"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "USER")
    void addSubtitleToFilmRejectsNonAdminUser() throws Exception {
        mockMvc.perform(post("/api/film/add/subtitle/3/6"))
                .andExpect(status().isForbidden());
    }

    @Test
    void removeSubtitleFromFilmRejectsAnonymousUser() throws Exception {
        mockMvc.perform(delete("/api/film/remove/subtitle/3/6"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "USER")
    void removeSubtitleFromFilmRejectsNonAdminUser() throws Exception {
        mockMvc.perform(delete("/api/film/remove/subtitle/3/6"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void uploadFilmRejectsMissingVideo() throws Exception {
        mockMvc.perform(multipart("/api/film/upload"))
                .andExpect(status().isBadRequest());
    }
}
