package zhedron.movie.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import zhedron.movie.dto.response.MediaContentResponse;
import zhedron.movie.dto.response.MessageResponse;
import zhedron.movie.dto.response.PaginatedResponse;
import zhedron.movie.dto.response.request.MediaContentRequest;
import zhedron.movie.enums.Status;
import zhedron.movie.services.MediaContentService;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mediacontent")
@Tag(name = "Media Content", description = "Endpoints for creating, updating, publishing, and linking films or seasons to media content")
public class MediaContentController {
    private final MediaContentService mediaContentService;

    public MediaContentController(MediaContentService mediaContentService) {
        this.mediaContentService = mediaContentService;
    }

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequestBody(content = @Content(encoding = @Encoding(name = "mediaContentRequest", contentType = MediaType.APPLICATION_JSON_VALUE)))
    @Operation(
            summary = "Create new media content",
            description = "Uploads media content details alongside cover art image files (JPEG/PNG)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Media content created successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MediaContentResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request payload or unsupported image format",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MessageResponse.class),
                            examples = {
                                    @ExampleObject(name = "Missing Image", value = "{\"message\": \"Image is required\"}"),
                                    @ExampleObject(name = "Invalid Content Type", value = "{\"message\": \"Invalid content type\"}"),
                                    @ExampleObject(name = "Empty File", value = "{\"message\": \"Upload image file\"}")
                            }
                    )
            )
    })
    public ResponseEntity<?> createMediaContent(@Valid @RequestPart MediaContentRequest mediaContentRequest, BindingResult bindingResult, @RequestPart List<MultipartFile> images) throws IOException {
        if (bindingResult.hasErrors()) {
            for (FieldError error : bindingResult.getFieldErrors()) {
                Map<String, String> errorMap = new HashMap<>();

                errorMap.put("message", error.getDefaultMessage());

                return new ResponseEntity<>(errorMap, HttpStatus.BAD_REQUEST);
            }
        }

        for (MultipartFile image : images) {
            if (image == null) {
                return ResponseEntity.badRequest().body(new MessageResponse("Image is required"));
            } else if (!image.getContentType().equals("image/jpeg") && !image.getContentType().equals("image/png")) {
                return ResponseEntity.badRequest().body(new MessageResponse("Invalid content type"));
            } else if (image.isEmpty()) {
                return ResponseEntity.badRequest().body(new MessageResponse("Upload image file"));
            }
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(mediaContentService.createMediaContent(mediaContentRequest, images));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get media content by ID",
            description = "Retrieves details for a specific media content entry and dynamically recalculates duration if linked to episodes."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Media content found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MediaContentResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Media content not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                        examples = @ExampleObject(name = "Not Found", value = "{\"message\": \"Media Content not found with id 1\"}")
                    )
            )
    })
    public ResponseEntity<MediaContentResponse> getMediaContentById(@PathVariable long id) {
        return ResponseEntity.ok(mediaContentService.findById(id));
    }

    @DeleteMapping("/delete/{id}")
    @Operation(
            summary = "Delete media content by ID",
            description = "Deletes a media content entry by its unique identifier."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Media content deleted successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MessageResponse.class),
                            examples = @ExampleObject(name = "Success", value = "{\"message\": \"Media Content has been deleted\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Media content not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "Not Found", value = "{\"message\": \"Media Content not found with id 1\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Title or description is missing or blank",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MessageResponse.class),
                            examples = @ExampleObject(name = "Invalid Request", value = "{\"message\": \"Write title, title must not be empty\"}")
                    )
            )
    })
    public ResponseEntity<MessageResponse> deleteMediaContentById(@PathVariable long id) {
        mediaContentService.deleteById(id);

        return ResponseEntity.ok(new MessageResponse("Media Content has been deleted"));
    }

    @PutMapping("/change-status/{id}")
    @Operation(
            summary = "Change media content visibility status",
            description = "Updates the visibility status (e.g., PUBLIC, PRIVATE) of specified media content."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Status updated successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MediaContentResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Media content not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "Not Found", value = "{\"message\": \"Media Content not found with id 1\"}")
                    )
            )
    })
    public ResponseEntity<MediaContentResponse> changeMediaContentRole(@PathVariable long id, @RequestParam Status status) {
        return ResponseEntity.ok(mediaContentService.changeVisibility(status, id));
    }

    @PostMapping("/film")
    @Operation(
            summary = "Add film to media content",
            description = "Links a film video to media content and auto-calculates total duration."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Film successfully added to media content",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MediaContentResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Conflict — Media content already contains a film or seasons",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "Conflict", value = "{\"message\": \"Episodes/Film already exists in Media Content\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Media content or film not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(name = "Media Content Not Found", value = "{\"message\": \"Media Content not found with id 1\"}"),
                                    @ExampleObject(name = "Film Not Found", value = "{\"message\": \"Film not found with id 10\"}")
                            }
                    )
            )
    })
    public ResponseEntity<MediaContentResponse> addFilmToMediaContent(@RequestParam long mediaContentId, @RequestParam long filmId) {
        return ResponseEntity.ok(mediaContentService.addFilmToMediaContent(mediaContentId, filmId));
    }

    @PostMapping("/season")
    @Operation(
            summary = "Add season to media content",
            description = "Links a TV series season to media content and recalculates aggregate duration from episodes."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Season successfully added to media content",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MediaContentResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Conflict — Media content already contains a film or seasons",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "Conflict", value = "{\"message\": \"Episodes/Film already exists in Media Content\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Media content or season not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(name = "Media Content Not Found", value = "{\"message\": \"Media Content not found with id 1\"}"),
                                    @ExampleObject(name = "Season Not Found", value = "{\"message\": \"Season not found with id 3\"}")
                            }
                    )
            )
    })
    public ResponseEntity<MediaContentResponse> addSeasonToMediaContent(@RequestParam long mediaContentId, @RequestParam long seasonId) {
        return ResponseEntity.ok(mediaContentService.addSeasonToMediaContent(mediaContentId, seasonId));
    }

    @DeleteMapping("/film/delete")
    @Operation(
            summary = "Remove film from media content",
            description = "Unlinks a film from media content and resets content duration to zero."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Film removed successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MessageResponse.class),
                            examples = @ExampleObject(name = "Success", value = "{\"message\": \"Film has been deleted from Media Content\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Media content, film, or association not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(name = "Media Content Not Found", value = "{\"message\": \"Media Content not found with id 1\"}"),
                                    @ExampleObject(name = "Film Not Found", value = "{\"message\": \"Film not found with id 10\"}"),
                                    @ExampleObject(name = "Film Not In Media", value = "{\"message\": \"Film not found in Media Content\"}")
                            }
                    )
            )
    })
    public ResponseEntity<MessageResponse> deleteFilmFromMediaContent(@RequestParam long mediaContentId, @RequestParam long filmId) {
        mediaContentService.deleteFilmFromMediaContent(mediaContentId, filmId);

        return ResponseEntity.ok(new MessageResponse("Film has been deleted from Media Content"));
    }

    @DeleteMapping("/season/delete")
    @Operation(
            summary = "Remove season from media content",
            description = "Unlinks a season from media content and recalculates remaining total duration."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Season removed successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MessageResponse.class),
                            examples = @ExampleObject(name = "Success", value = "{\"message\": \"Season has been deleted from Media Content\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Media content, season, or association not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(name = "Media Content Not Found", value = "{\"message\": \"Media Content not found with id 1\"}"),
                                    @ExampleObject(name = "Season Not Found", value = "{\"message\": \"Season not found with id 3\"}"),
                                    @ExampleObject(name = "Season Not In Media", value = "{\"message\": \"Seasons not found in Media Content\"}")
                            }
                    )
            )
    })
    public ResponseEntity<MessageResponse> deleteSeasonFromMediaContent(@RequestParam long mediaContentId, @RequestParam long seasonId) {
        mediaContentService.deleteSeasonFromMediaContent(mediaContentId, seasonId);

        return ResponseEntity.ok(new MessageResponse("Season has been deleted from Media Content"));
    }

    @PutMapping("/update/{id}")
    @Operation(
            summary = "Update media content details",
            description = "Updates specific details of existing media content."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Media content updated successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MediaContentResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Media content not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "Not Found", value = "{\"message\": \"Media Content not found with id 1\"}")
                    )
            )
    })
    public ResponseEntity<?> updateMediaContent(@PathVariable long id, @Valid @RequestBody MediaContentRequest updateContentMedia, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            for (FieldError error : bindingResult.getFieldErrors()) {
                Map<String, String> errorMap = new HashMap<>();

                errorMap.put("message", error.getDefaultMessage());

                return new ResponseEntity<>(errorMap, HttpStatus.BAD_REQUEST);
            }
        }

        return ResponseEntity.ok(mediaContentService.updateMediaContent(updateContentMedia, id));
    }

    @GetMapping()
    @Operation(
            summary = "Get all public media contents",
            description = "Retrieves a paginated list of media contents that have PUBLIC status, ordered by release date descending."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Paginated list retrieved successfully",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = PaginatedResponse.class)
            )
    )
    public ResponseEntity<PaginatedResponse> getAllMediaContents(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(mediaContentService.findAll(page, size));
    }

    @GetMapping("/start-date/end-date")
    @Operation(
            summary = "Get media contents released between start and end date",
            description = "Retrieves a paginated list of public media contents released within a specific date range."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Filtered paginated list retrieved successfully",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = PaginatedResponse.class)
            )
    )
    public ResponseEntity<PaginatedResponse> getMediaContentsBetweenStartReleaseDateAndEndReleaseDate(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, @RequestParam LocalDate startDate, @RequestParam LocalDate endDate) {
        return ResponseEntity.ok(mediaContentService.getMediaContentsBetweenStartReleaseDateAndEndReleaseDate(page, size, startDate, endDate));
    }

    @PostMapping("/actor/{mediaContentId}/{actorId}")
    @Operation(
            summary = "Add actor to media content",
            description = "Links an existing actor to a media content entry. The actor must not already be linked."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Actor added successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MediaContentResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Actor is already linked to the media content",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "object", implementation = Map.class),
                            examples = @ExampleObject(value = "{\"message\": \"Actor already exists in Media Content\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Actor or media content not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "object", implementation = Map.class),
                            examples = @ExampleObject(value = "{\"message\": \"Media Content not found with 1\"}")
                    )
            )
    })
    public ResponseEntity<MediaContentResponse> addActorToMediaContent(@PathVariable long mediaContentId, @PathVariable long actorId) {
        return ResponseEntity.ok(mediaContentService.addActorToMediaContent(mediaContentId, actorId));
    }

    @DeleteMapping("/delete/actor/{mediaContentId}/{actorId}")
    @Operation(
            summary = "Remove actor from media content",
            description = "Unlinks an actor from a media content entry. The association must exist."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Actor removed successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MediaContentResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Actor, media content, or actor association not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "object", implementation = Map.class),
                            examples = {
                                    @ExampleObject(name = "Media Content Not Found", value = "{\"message\": \"Media Content not found with 1\"}"),
                                    @ExampleObject(name = "Actor Not Linked", value = "{\"message\": \"Actor not exists in Media Content\"}")
                            }
                    )
            )
    })
    public ResponseEntity<MediaContentResponse> removeActorFromMediaContent(@PathVariable long mediaContentId, @PathVariable long actorId) {
        return ResponseEntity.ok(mediaContentService.removeActorFromMediaContent(mediaContentId, actorId));
    }

    @GetMapping("/stream/photo/{id}")
    @Operation(
            summary = "Stream media content cover art",
            description = "Streams the selected cover-art image for a media content entry by its zero-based cover-art index."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Cover-art image streamed successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                            schema = @Schema(type = "string", format = "binary")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Image not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MessageResponse.class),
                            examples = @ExampleObject(name = "Image Load Failure", value = "{\"message\": \"Not found image\"}")
                    )
            )
    })
    public ResponseEntity<?> streamPhotoMediaContent(@PathVariable long id, @RequestParam int size) {
        try {
            MediaContentResponse mediaContentResponse = mediaContentService.findById(id);

            String fileName = mediaContentResponse.coverArts().get(size - 1);

            Path path = Paths.get("cover_art/").resolve(fileName).normalize();

            Resource resource = new UrlResource(path.toUri());

            return ResponseEntity.status(HttpStatus.OK)
                    .contentType(MediaType.parseMediaType(mediaContentResponse.contentTypes().get(size - 1)))
                    .body(resource);
        } catch (IndexOutOfBoundsException | IOException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("Not found image"));
        }
    }
}
