package zhedron.movie.services.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import zhedron.movie.dto.response.ActorResponse;
import zhedron.movie.dto.response.request.ActorRequest;
import zhedron.movie.entity.Actor;
import zhedron.movie.entity.MediaContent;
import zhedron.movie.enums.Gender;
import zhedron.movie.exceptions.ActorNotFoundException;
import zhedron.movie.mappers.ActorMapper;
import zhedron.movie.repository.ActorRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActorServiceImplTest {
    private static final Path CREATED_IMAGE = Path.of("images", "Ada_portrait.png");
    @Mock
    private ActorRepository actorRepository;
    @Mock
    private ActorMapper actorMapper;
    @InjectMocks
    private ActorServiceImpl actorService;

    @AfterEach
    void cleanCreatedImage() throws IOException {
        Files.deleteIfExists(CREATED_IMAGE);
    }

    @Test
    void createActorStoresUploadedImageAndSavesActor() throws Exception {
        ActorRequest request = actorRequest();
        MultipartFile image = new MockMultipartFile("images", "portrait.png", "image/png", "image".getBytes());
        Actor savedActor = actor(12L);
        ActorResponse expectedResponse = actorResponse(12L);
        when(actorRepository.save(any(Actor.class))).thenReturn(savedActor);
        when(actorMapper.toActorResponse(savedActor)).thenReturn(expectedResponse);

        ActorResponse response = actorService.createActor(request, List.of(image));

        ArgumentCaptor<Actor> actorCaptor = ArgumentCaptor.forClass(Actor.class);
        verify(actorRepository).save(actorCaptor.capture());
        Actor actorToSave = actorCaptor.getValue();
        assertEquals("Ada", actorToSave.getName());
        assertEquals("Lovelace", actorToSave.getSurname());
        assertEquals(LocalDate.now().getYear() - 1815, actorToSave.getAge());
        assertEquals(List.of("Ada_portrait.png"), actorToSave.getPhotos());
        assertEquals(List.of("image/png"), actorToSave.getContentTypes());
        assertTrue(Files.exists(CREATED_IMAGE));
        assertSame(expectedResponse, response);
    }

    @Test
    void updateActorChangesOnlyProvidedFields() {
        Actor existingActor = actor(12L);
        ActorRequest request = new ActorRequest();
        request.setSurname("Byron");
        request.setYear(1816);
        ActorResponse expectedResponse = actorResponse(12L);
        when(actorRepository.findById(12L)).thenReturn(Optional.of(existingActor));
        when(actorRepository.save(existingActor)).thenReturn(existingActor);
        when(actorMapper.toActorResponse(existingActor)).thenReturn(expectedResponse);

        ActorResponse response = actorService.updateActor(request, 12L);

        assertEquals("Ada", existingActor.getName());
        assertEquals("Byron", existingActor.getSurname());
        assertEquals(1816, existingActor.getYear());
        assertEquals(LocalDate.now().getYear() - 1816, existingActor.getAge());
        assertSame(expectedResponse, response);
    }

    @Test
    void deleteByIdRemovesActorFromLinkedMediaContent() {
        Actor actor = actor(12L);
        MediaContent mediaContent = new MediaContent();
        mediaContent.setActors(new ArrayList<>(List.of(actor)));
        actor.setMediaContents(List.of(mediaContent));
        when(actorRepository.existsById(12L)).thenReturn(true);
        when(actorRepository.findById(12L)).thenReturn(Optional.of(actor));

        actorService.deleteById(12L);

        assertFalse(mediaContent.getActors().contains(actor));
        verify(actorRepository).deleteById(12L);
    }

    @Test
    void deleteByIdThrowsWhenActorDoesNotExist() {
        when(actorRepository.existsById(404L)).thenReturn(false);

        assertThrows(ActorNotFoundException.class, () -> actorService.deleteById(404L));

        verify(actorRepository, never()).deleteById(404L);
    }

    @Test
    void findByIdThrowsWhenActorDoesNotExist() {
        when(actorRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ActorNotFoundException.class, () -> actorService.findById(404L));
    }

    @Test
    void uploadAvatarStoresPhotoAndContentType() throws Exception {
        Actor existingActor = actor(12L);
        MultipartFile image = new MockMultipartFile("image", "new.png", "image/png", "image".getBytes());
        ActorResponse expectedResponse = actorResponse(12L);
        when(actorRepository.findById(12L)).thenReturn(Optional.of(existingActor));
        when(actorRepository.save(existingActor)).thenReturn(existingActor);
        when(actorMapper.toActorResponse(existingActor)).thenReturn(expectedResponse);

        ActorResponse response = actorService.uploadAvatarById(12L, image);

        assertEquals(List.of("Ada_new.png"), existingActor.getPhotos());
        assertEquals(List.of("image/png"), existingActor.getContentTypes());
        assertTrue(Files.exists(Path.of("images", "Ada_new.png")));
        assertSame(expectedResponse, response);
        verify(actorRepository).save(existingActor);
        Files.deleteIfExists(Path.of("images", "Ada_new.png"));
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
        actor.setMediaContents(new ArrayList<>());
        actor.setPhotos(new ArrayList<>());
        actor.setContentTypes(new ArrayList<>());
        return actor;
    }

    private static ActorResponse actorResponse(long id) {
        return new ActorResponse(id, "Ada", "Lovelace", 1815, 36, List.of(), List.of("Ada_portrait.png"), List.of("image/png"), Gender.FEMALE);
    }
}
