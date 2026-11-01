package zhedron.movie.services;

import org.springframework.web.multipart.MultipartFile;
import zhedron.movie.dto.response.ActorResponse;
import zhedron.movie.dto.response.request.ActorRequest;
import zhedron.movie.entity.Actor;

import java.io.IOException;
import java.util.List;

public interface ActorService {
    ActorResponse createActor(ActorRequest actorRequest, List<MultipartFile> images) throws IOException;

    ActorResponse updateActor(ActorRequest actorRequest, long id);

    void deleteById(long id);

    Actor findById(long id);

    ActorResponse uploadAvatarById(long id, MultipartFile image) throws IOException;
}
