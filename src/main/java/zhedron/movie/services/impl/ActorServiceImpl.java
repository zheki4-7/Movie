package zhedron.movie.services.impl;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import zhedron.movie.dto.response.ActorResponse;
import zhedron.movie.dto.response.request.ActorRequest;
import zhedron.movie.entity.Actor;
import zhedron.movie.entity.MediaContent;
import zhedron.movie.exceptions.ActorNotFoundException;
import zhedron.movie.mappers.ActorMapper;
import zhedron.movie.repository.ActorRepository;
import zhedron.movie.services.ActorService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ActorServiceImpl implements ActorService {
    private final ActorRepository actorRepository;

    private final ActorMapper actorMapper;

    private final String DIRECTORY = "images/";

    public ActorServiceImpl(ActorRepository actorRepository, ActorMapper actorMapper) {
        this.actorRepository = actorRepository;
        this.actorMapper = actorMapper;
    }

    @Override
    @CachePut(value = "actors", key = "#result.id()")
    public ActorResponse createActor(ActorRequest actorRequest, List<MultipartFile> images) throws IOException {
        Actor actor = new Actor();

        actor.setName(actorRequest.getName());
        actor.setSurname(actorRequest.getSurname());
        actor.setYear(actorRequest.getYear());
        actor.setAge(LocalDate.now().getYear() - actorRequest.getYear());
        actor.setGender(actorRequest.getGender());

        Path path = Paths.get(DIRECTORY);

        if (Files.notExists(path)) {
            Files.createDirectories(path);
        }

        List<String> listImages = new ArrayList<>();

        List<String> contentTypes = new ArrayList<>();

        for (MultipartFile image : images) {
            String fileName = actor.getName() + "_" +  image.getOriginalFilename();

            listImages.add(fileName);

            contentTypes.add(image.getContentType());

            Path imagePath = path.resolve(fileName).normalize();

            Files.copy(image.getInputStream(), imagePath, StandardCopyOption.REPLACE_EXISTING);
        }

        actor.setPhotos(listImages);
        actor.setContentTypes(contentTypes);

        Actor savedActor = actorRepository.save(actor);

        return actorMapper.toActorResponse(savedActor);
    }

    @Override
    @Cacheable(value = "actors", key = "#id")
    public ActorResponse updateActor(ActorRequest actorRequest, long id) {
        Actor actor = findById(id);

        if (actorRequest.getName() != null) {
            actor.setName(actorRequest.getName());
        }
        if (actorRequest.getSurname() != null) {
            actor.setSurname(actorRequest.getSurname());
        }
        if (actorRequest.getYear() != null) {
            actor.setYear(actorRequest.getYear());
            actor.setAge(LocalDate.now().getYear() - actorRequest.getYear());
        }

        Actor updatedActor = actorRepository.save(actor);

        return actorMapper.toActorResponse(updatedActor);
    }

    @Override
    @CacheEvict(value = "actors", key = "#id")
    public void deleteById(long id) {
        if (!actorRepository.existsById(id)) {
            throw new ActorNotFoundException("Actor not found with + id");
        }

        Actor actor = findById(id);

        if (!actor.getMediaContents().isEmpty()) {
            for (MediaContent mediaContent : actor.getMediaContents()) {
                mediaContent.getActors().remove(actor);
            }
        }

        actorRepository.deleteById(id);
    }

    @Override
    @Cacheable(value = "actors", key = "#id")
    public Actor findById(long id) {
        return actorRepository.findById(id).orElseThrow(() -> new ActorNotFoundException("Actor not found with " + id));
    }

    @Override
    @Cacheable(value = "actors", key = "#id")
    public ActorResponse uploadAvatarById(long id, MultipartFile image) throws IOException {
        Actor actor = findById(id);

        String fileName = actor.getName() + "_" + image.getOriginalFilename();

        Path path = Paths.get(DIRECTORY).resolve(fileName).normalize();

        actor.getPhotos().add(fileName);
        actor.getContentTypes().add(image.getContentType());

        Files.copy(image.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

        Actor uploadedActor = actorRepository.save(actor);

        return actorMapper.toActorResponse(uploadedActor);
    }
}
