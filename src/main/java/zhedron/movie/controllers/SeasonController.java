package zhedron.movie.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import zhedron.movie.dto.response.MessageResponse;
import zhedron.movie.dto.response.SeasonResponse;
import zhedron.movie.dto.response.request.SeasonCreateRequest;
import zhedron.movie.services.SeasonService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/season")
@Tag(name = "Seasons", description = "Endpoints for creating, managing, and linking episodes to seasons")
public class SeasonController {
    private final SeasonService seasonService;

    public SeasonController(SeasonService seasonService) {
        this.seasonService = seasonService;
    }

    @PostMapping("/create")
    @Operation(
            summary = "Create a new season",
            description = "Creates a new season record with a designated season number."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Season created successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SeasonResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error in request payload",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "object", implementation = Map.class),
                            examples = @ExampleObject(
                                    name = "Validation Error",
                                    value = "{\"message\": \"Enter a number\"}"
                            )
                    )
            )
    })
    public ResponseEntity<?> createSeason(@RequestBody @Valid SeasonCreateRequest seasonCreateRequest, BindingResult bindingResult) {
        if  (bindingResult.hasErrors()) {
            for (FieldError error : bindingResult.getFieldErrors()) {
                Map<String, String> errors = new HashMap<>();

                errors.put("message", error.getDefaultMessage());

                return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(seasonService.createSeason(seasonCreateRequest));
    }

    @DeleteMapping("/delete/{id}")
    @Operation(
            summary = "Delete a season by ID",
            description = "Deletes a season record by its unique ID."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Season deleted successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MessageResponse.class),
                            examples = @ExampleObject(name = "Success", value = "{\"message\": \"Season deleted successfully\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Season not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "Not Found", value = "{\"message\": \"Season not found with 1\"}")
                    )
            )
    })
    public ResponseEntity<MessageResponse> deleteSeasonById(@PathVariable long id) {
        seasonService.deleteById(id);

        return ResponseEntity.ok(new MessageResponse("Season deleted successfully"));
    }

    @GetMapping("{id}")
    @Operation(
            summary = "Get season by ID",
            description = "Retrieves season details along with its associated episodes list."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Season retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SeasonResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Season not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "Not Found", value = "{\"message\": \"Season not found with 1\"}")
                    )
            )
    })
    public ResponseEntity<SeasonResponse> getSeasonById(@PathVariable long id) {
        return ResponseEntity.ok(seasonService.findById(id));
    }

    @PostMapping("/add/episode")
    @Operation(
            summary = "Add episode to a season",
            description = "Links an existing episode to a specified season using request parameters."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Episode added to season successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SeasonResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Season or Episode not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(name = "Season Not Found", value = "{\"message\": \"Season not found with 1\"}"),
                                    @ExampleObject(name = "Episode Not Found", value = "{\"message\": \"Episode not found with 5\"}")
                            }
                    )
            )
    })
    public ResponseEntity<SeasonResponse> addEpisode(@RequestParam long seasonId, @RequestParam long episodeId) {
        return ResponseEntity.ok(seasonService.addEpisodeToSeason(seasonId, episodeId));
    }

    @DeleteMapping("/delete/episode")
    @Operation(
            summary = "Remove episode from a season",
            description = "Unlinks an episode from a specified season."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Episode removed from season successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MessageResponse.class),
                            examples = @ExampleObject(name = "Success", value = "{\"message\": \"Episode has been removed from Season\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Season, Episode, or association not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(name = "Season Not Found", value = "{\"message\": \"Season not found with 1\"}"),
                                    @ExampleObject(name = "Episode Not Found", value = "{\"message\": \"Episode not found with 5\"}"),
                                    @ExampleObject(name = "Episode Not In Season", value = "{\"message\": \"Episode not found in Season\"}")
                            }
                    )
            )
    })
    public ResponseEntity<MessageResponse> removeEpisodeFromSeason(@RequestParam long seasonId, @RequestParam long episodeId) {
        seasonService.removeEpisodeFromSeason(seasonId, episodeId);

        return ResponseEntity.ok(new MessageResponse("Episode has been removed from Season"));
    }
}
