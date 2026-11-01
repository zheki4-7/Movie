package zhedron.movie.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import zhedron.movie.dto.response.ActorResponse;
import zhedron.movie.dto.response.request.ActorRequest;
import zhedron.movie.entity.Actor;
import zhedron.movie.enums.Gender;
import zhedron.movie.mappers.ActorMapper;
import zhedron.movie.repository.UserRepository;
import zhedron.movie.services.ActorService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Base64;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ActorController.class)
@Import({SecurityConfig.class, ControllerSecurityTestConfig.class})
class ActorControllerTest {
    private static final Path STREAM_IMAGE = Path.of("images", "actor-controller-stream.png");
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ActorService actorService;
    @MockitoBean
    private ActorMapper actorMapper;
    @MockitoBean
    private UserRepository userRepository;

    @AfterEach
    void cleanStreamImage() throws IOException {
        Files.deleteIfExists(STREAM_IMAGE);
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void createActorAcceptsMultipartRequest() throws Exception {
        MockMultipartFile requestPart = new MockMultipartFile(
                "actorRequest", "", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(actorRequest()));
        MockMultipartFile image = new MockMultipartFile("images", "portrait.png", "image/png", pngBytes());
        when(actorService.createActor(any(ActorRequest.class), any())).thenReturn(actorResponse(5L));

        mockMvc.perform(multipart("/api/actor/create").file(requestPart).file(image))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("Ada"));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void createActorRejectsInvalidImageType() throws Exception {
        MockMultipartFile requestPart = new MockMultipartFile(
                "actorRequest", "", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(actorRequest()));
        MockMultipartFile image = new MockMultipartFile("images", "portrait.gif", "image/gif", "image".getBytes());

        mockMvc.perform(multipart("/api/actor/create").file(requestPart).file(image))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid image type"));
    }

    @Test
    void findActorByIdReturnsMappedResponse() throws Exception {
        Actor actor = actor(5L);
        when(actorService.findById(5L)).thenReturn(actor);
        when(actorMapper.toActorResponse(actor)).thenReturn(actorResponse(5L));

        mockMvc.perform(get("/api/actor/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.surname").value("Lovelace"));
    }

    @Test
    void streamPhotosUsesRequestedPhotoContentType() throws Exception {
        Files.createDirectories(STREAM_IMAGE.getParent());
        Files.write(STREAM_IMAGE, "image".getBytes());
        Actor actor = actor(5L);
        actor.setPhotos(List.of(STREAM_IMAGE.getFileName().toString()));
        actor.setContentTypes(List.of(MediaType.IMAGE_PNG_VALUE));
        when(actorService.findById(5L)).thenReturn(actor);

        mockMvc.perform(get("/api/actor/stream/5").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void deleteActorReturnsMessage() throws Exception {
        mockMvc.perform(delete("/api/actor/delete/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Successfully deleted actor"));

        verify(actorService).deleteById(5L);
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void updateActorReturnsServiceResponse() throws Exception {
        ActorRequest request = actorRequest();
        when(actorService.updateActor(any(ActorRequest.class), org.mockito.ArgumentMatchers.eq(5L)))
                .thenReturn(actorResponse(5L));

        mockMvc.perform(patch("/api/actor/update/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.gender").value("FEMALE"));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void uploadPhotoReturnsUpdatedActor() throws Exception {
        when(actorService.uploadAvatarById(org.mockito.ArgumentMatchers.eq(5L), any())).thenReturn(actorResponse(5L));

        MockMultipartFile image = new MockMultipartFile("image", "portrait.png", "image/png", pngBytes());

        mockMvc.perform(multipart("/api/actor/upload/5").file(image))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void uploadPhotoRejectsEmptyImage() throws Exception {
        MockMultipartFile image = new MockMultipartFile("image", "portrait.png", "image/png", new byte[0]);

        mockMvc.perform(multipart("/api/actor/upload/5").file(image))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Upload image"));
    }

    private static byte[] pngBytes() {
        return Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");
    }

    private static ActorRequest actorRequest() {
        ActorRequest request = new ActorRequest();
        request.setName("Ada");
        request.setSurname("Lovelace");
        request.setYear(1815);
        request.setGender(Gender.FEMALE);
        return request;
    }

    private static Actor actor(long id) {
        Actor actor = new Actor();
        actor.setId(id);
        actor.setName("Ada");
        actor.setSurname("Lovelace");
        actor.setYear(1815);
        actor.setAge(36);
        actor.setGender(Gender.FEMALE);
        return actor;
    }

    private static ActorResponse actorResponse(long id) {
        return new ActorResponse(id, "Ada", "Lovelace", 1815, 36, List.of(), List.of("portrait.jpg"), List.of("image/jpeg"), Gender.FEMALE);
    }
}
