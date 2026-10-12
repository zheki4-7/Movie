package zhedron.movie.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import zhedron.movie.dto.response.MessageResponse;
import zhedron.movie.dto.response.SubtitleResponse;
import zhedron.movie.entity.Subtitle;
import zhedron.movie.mappers.SubtitleMapper;
import zhedron.movie.repository.SubtitleRepository;
import zhedron.movie.services.SubtitleService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subtitle")
@Tag(name = "Subtitles", description = "Endpoints for uploading, reading, and deleting SRT subtitle files")
public class SubtitleController {
    private final SubtitleService subtitleService;

    private final SubtitleRepository subtitleRepository;

    private final SubtitleMapper subtitleMapper;

    public SubtitleController(SubtitleService subtitleService, SubtitleRepository subtitleRepository, SubtitleMapper subtitleMapper) {
        this.subtitleService = subtitleService;
        this.subtitleRepository = subtitleRepository;
        this.subtitleMapper = subtitleMapper;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get subtitle metadata", description = "Returns subtitle metadata and the film or episode to which it is linked.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Subtitle found", content = @Content(schema = @Schema(implementation = SubtitleResponse.class))),
            @ApiResponse(responseCode = "404", description = "Subtitle not found", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(value = "{\"message\": \"Subtitle not found with 1\"}")))
    })
    public SubtitleResponse getSubtitle(@PathVariable int id) {
        Subtitle subtitle = subtitleService.findById(id);

        return subtitleMapper.toSubtitleResponse(subtitle);
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "Delete a subtitle", description = "Deletes a subtitle record by ID. The stored SRT file is not removed by this operation.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Subtitle deleted", content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "404", description = "Subtitle not found", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(value = "{\"message\": \"Subtitle not found with 1\"}")))
    })
    public ResponseEntity<MessageResponse> deleteSubtitle(@PathVariable int id) {
        subtitleService.deleteSubtitleById(id);

        return ResponseEntity.ok(new MessageResponse("Subtitle deleted successfully"));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload an SRT subtitle", description = "Uploads an SRT subtitle file and returns its stored metadata.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Subtitle uploaded", content = @Content(schema = @Schema(implementation = SubtitleResponse.class))),
            @ApiResponse(responseCode = "400", description = "Missing, empty, or non-SRT file", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = MessageResponse.class), examples = {
                    @ExampleObject(name = "Empty File", value = "{\"message\": \"Upload file\"}"),
                    @ExampleObject(name = "Invalid Format", value = "{\"message\": \"Upload srt file\"}")
            }))
    })
    public ResponseEntity<?> uploadFile(@RequestPart MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse("Upload file"));
        } else if (!file.getContentType().equals("application/x-subrip")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse("Upload srt file"));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(subtitleService.uploadFile(file));
    }

    @GetMapping("/datasrt/film/{filmId}")
    @Operation(summary = "Read film subtitle cues", description = "Parses the SRT file linked to a film and returns its timed subtitle cues.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Subtitle cues returned", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Film or subtitle not found"),
            @ApiResponse(responseCode = "500", description = "SRT file could not be parsed", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(value = "{\"message\": \"Failed to read file\"}")))
    })
    public List<Map<String, Object>> getDataSrt(@PathVariable int filmId) {
        Subtitle subtitle = subtitleRepository.findByFilmId(filmId);

        return subtitleService.getDataFromSrtFile(Path.of(subtitle.getSrtName()));
    }

    @GetMapping("/datasrt/episode/{episodeId}")
    @Operation(summary = "Read episode subtitle cues", description = "Parses the SRT file linked to an episode and returns its timed subtitle cues.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Subtitle cues returned", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Episode or subtitle not found"),
            @ApiResponse(responseCode = "500", description = "SRT file could not be parsed", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(value = "{\"message\": \"Failed to read file\"}")))
    })
    public List<Map<String, Object>> getDataEpisode(@PathVariable int episodeId) {
        Subtitle subtitle = subtitleRepository.findByEpisodeId(episodeId);

        return subtitleService.getDataFromSrtFile(Path.of(subtitle.getSrtName()));
    }
}
