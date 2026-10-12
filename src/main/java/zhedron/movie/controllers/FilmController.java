package zhedron.movie.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import zhedron.movie.dto.response.FilmResponse;
import zhedron.movie.dto.response.MessageResponse;
import zhedron.movie.services.FilmService;
import zhedron.movie.services.SubtitleService;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/film")
@Tag(name = "Films", description = "Endpoints for uploading, streaming, and deleting film video files")
public class FilmController {
    private final FilmService filmService;
    private final SubtitleService subtitleService;

    public FilmController(FilmService filmService, SubtitleService subtitleService) {
        this.filmService = filmService;
        this.subtitleService = subtitleService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload a film video file",
            description = "Uploads a single video file in MP4 format."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Film uploaded successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FilmResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid upload request (missing file, empty file, or non-MP4 format)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MessageResponse.class),
                            examples = {
                                    @ExampleObject(name = "Missing Video", value = "{\"message\": \"Video is required\"}"),
                                    @ExampleObject(name = "Empty File", value = "{\"message\": \"Upload video file\"}"),
                                    @ExampleObject(name = "Invalid Format", value = "{\"message\": \"Upload video file with mp4 format\"}")
                            }
                    )
            )
    })
    public ResponseEntity<?> uploadFilm(@RequestPart MultipartFile video) throws IOException {
        if (video == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Video is required"));
        } else if (video.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Upload video file"));
        } else if (!video.getContentType().equals("video/mp4")) {
            return ResponseEntity.badRequest().body(new MessageResponse("Upload video file with mp4 format"));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(filmService.uploadFilm(video));
    }

    @DeleteMapping("/delete/{id}")
    @Operation(
            summary = "Delete a film by ID",
            description = "Deletes the film record, unlinks it from associated media content, and removes the stored video file."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Film deleted successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MessageResponse.class),
                            examples = @ExampleObject(name = "Success", value = "{\"message\": \"Film deleted successfully\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Film not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "Not Found", value = "{\"message\": \"Film not found with id 1\"}")
                    )
            )
    })
    public ResponseEntity<MessageResponse> deleteById(@PathVariable long id) {
        filmService.deleteById(id);

        return ResponseEntity.ok(new MessageResponse("Film deleted successfully"));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Stream film video by ID",
            description = "Retrieves and streams the video file resource for the given film ID."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Video stream file content",
                    content = @Content(
                            mediaType = "video/mp4",
                            schema = @Schema(type = "string", format = "binary")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Malformed file URL or file load failure",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MessageResponse.class),
                            examples = @ExampleObject(name = "Load Error", value = "{\"message\": \"Failed to load video file\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Film not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "Not Found", value = "{\"message\": \"Film not found with 1\"}")
                    )
            )
    })
    public ResponseEntity<?> streamFileById(@PathVariable long id) {
        FilmResponse foundFilm = filmService.findById(id);

        try {
            Path path = Paths.get("film/").resolve(foundFilm.videoUrl()).normalize();

            Resource resource = new UrlResource(path.toUri());

            return ResponseEntity.status(HttpStatus.OK)
                    .contentType(MediaType.parseMediaType(foundFilm.contentType()))
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Failed to load video file"));
        }
    }

    @PostMapping("/add/subtitle/{subtitleId}/{filmId}")
    @Operation(summary = "Add subtitle to a film", description = "Links an existing subtitle to a film and marks it as released.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Subtitle added", content = @Content(schema = @Schema(implementation = FilmResponse.class))),
            @ApiResponse(responseCode = "404", description = "Film or subtitle not found"),
            @ApiResponse(responseCode = "400", description = "Subtitle already linked to film")
    })
    public FilmResponse addSubtitle(@PathVariable long subtitleId, @PathVariable long filmId) {
        return filmService.addSubtitleToFilm(subtitleId, filmId);
    }

    @DeleteMapping("/remove/subtitle/{subtitleId}/{filmId}")
    @Operation(summary = "Remove subtitle from a film", description = "Unlinks an existing subtitle from a film.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Subtitle removed", content = @Content(schema = @Schema(implementation = FilmResponse.class))),
            @ApiResponse(responseCode = "404", description = "Film, subtitle, or association not found")
    })
    public FilmResponse removeSubtitle(@PathVariable long subtitleId, @PathVariable long filmId) {
        return filmService.removeSubtitleFromFilm(subtitleId, filmId);
    }
}
