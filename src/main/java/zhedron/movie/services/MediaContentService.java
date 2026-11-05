package zhedron.movie.services;

import org.springframework.web.multipart.MultipartFile;
import zhedron.movie.dto.response.MediaContentResponse;
import zhedron.movie.dto.response.PaginatedResponse;
import zhedron.movie.dto.response.request.MediaContentRequest;
import zhedron.movie.enums.Status;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public interface MediaContentService {
    MediaContentResponse createMediaContent(MediaContentRequest mediaContentRequest, List<MultipartFile> images) throws IOException;

    MediaContentResponse findById(long id);

    void deleteById(long id);

    MediaContentResponse changeVisibility(Status status, long id);

    MediaContentResponse addFilmToMediaContent(long mediaContentId, long filmId);

    MediaContentResponse addSeasonToMediaContent(long mediaContentId, long seasonId);

    void removeFilmFromMediaContent(long mediaContentId, long filmId);

    void removeSeasonFromMediaContent(long mediaContentId, long seasonId);

    MediaContentResponse updateMediaContent(MediaContentRequest  mediaContentUpdateRequest, long id);

    PaginatedResponse findAll(int page, int size);

    PaginatedResponse getMediaContentsBetweenStartReleaseDateAndEndReleaseDate(int page, int size, LocalDate startReleaseDate, LocalDate endReleaseDate);

    MediaContentResponse addActorToMediaContent(long mediaContentId, long actorId);

    MediaContentResponse removeActorFromMediaContent(long mediaContentId, long actorId);

    MediaContentResponse uploadAvatarById(long id, MultipartFile image) throws IOException;

    PaginatedResponse findByTitle(int page, int size, String title);
}
