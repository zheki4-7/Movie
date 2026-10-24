package zhedron.movie.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import zhedron.movie.config.SecurityConfig;
import zhedron.movie.dto.response.FavoriteResponse;
import zhedron.movie.dto.response.request.FavoriteRequest;
import zhedron.movie.entity.Favorite;
import zhedron.movie.mappers.FavoriteMapper;
import zhedron.movie.repository.UserRepository;
import zhedron.movie.services.FavoriteService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FavoriteController.class)
@Import({SecurityConfig.class, ControllerSecurityTestConfig.class})
class FavoriteControllerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private FavoriteService favoriteService;
    @MockitoBean
    private FavoriteMapper favoriteMapper;
    @MockitoBean
    private UserRepository userRepository;

    @Test
    @WithMockUser
    void createFavoriteReturnsCreatedFavorite() throws Exception {
        when(favoriteService.create(any(FavoriteRequest.class))).thenReturn(response(4L, "Watch later"));

        mockMvc.perform(post("/api/favorite/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request("Watch later"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.name").value("Watch later"));
    }

    @Test
    @WithMockUser
    void getFavoriteRequiresAuthenticatedUser() throws Exception {
        Favorite favorite = new Favorite();
        favorite.setId(4L);
        favorite.setName("Watch later");
        when(favoriteService.findById(4L)).thenReturn(favorite);
        when(favoriteMapper.toFavoriteResponse(favorite)).thenReturn(response(4L, "Watch later"));

        mockMvc.perform(get("/api/favorite/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4));
    }

    @Test
    void getFavoriteRejectsAnonymousUser() throws Exception {
        mockMvc.perform(get("/api/favorite/4"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void getFavoriteByNameReturnsFavorite() throws Exception {
        when(favoriteService.getFavoriteByName("Watch later")).thenReturn(response(4L, "Watch later"));

        mockMvc.perform(get("/api/favorite/name").param("name", "Watch later"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.name").value("Watch later"));

        verify(favoriteService).getFavoriteByName("Watch later");
    }

    @Test
    void getFavoriteByNameRejectsAnonymousUser() throws Exception {
        mockMvc.perform(get("/api/favorite/name").param("name", "Watch later"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void updateFavoriteReturnsUpdatedFavorite() throws Exception {
        when(favoriteService.update(any(FavoriteRequest.class), eq(4L))).thenReturn(response(4L, "Favorites"));

        mockMvc.perform(put("/api/favorite/update/4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request("Favorites"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Favorites"));
    }

    @Test
    @WithMockUser
    void deleteFavoriteReturnsMessage() throws Exception {
        mockMvc.perform(delete("/api/favorite/delete/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Favorite deleted successfully"));

        verify(favoriteService).deleteFavorite(4L);
    }

    @Test
    @WithMockUser
    void addMediaContentReturnsUpdatedFavorite() throws Exception {
        when(favoriteService.addMediaContentToFavorite(4L, 9L)).thenReturn(response(4L, "Watch later"));

        mockMvc.perform(post("/api/favorite/add-media/4/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4));
    }

    @Test
    @WithMockUser
    void removeMediaContentReturnsUpdatedFavorite() throws Exception {
        when(favoriteService.removeMediaContentFromFavorite(4L, 9L)).thenReturn(response(4L, "Watch later"));

        mockMvc.perform(delete("/api/favorite/remove-media/4/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4));
    }

    @Test
    void createFavoriteRejectsAnonymousUser() throws Exception {
        mockMvc.perform(post("/api/favorite/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request("Watch later"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void createFavoriteReturnsValidationMessageForBlankName() throws Exception {
        mockMvc.perform(post("/api/favorite/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Name must not be empty"));

        verify(favoriteService, never()).create(any(FavoriteRequest.class));
    }

    @Test
    @WithMockUser
    void updateFavoriteReturnsValidationMessageForBlankName() throws Exception {
        mockMvc.perform(put("/api/favorite/update/4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Name must not be empty"));

        verify(favoriteService, never()).update(any(FavoriteRequest.class), eq(4L));
    }

    private static FavoriteRequest request(String name) {
        FavoriteRequest request = new FavoriteRequest();
        request.setName(name);
        return request;
    }

    private static FavoriteResponse response(long id, String name) {
        return new FavoriteResponse(id, name, List.of(), null);
    }
}
