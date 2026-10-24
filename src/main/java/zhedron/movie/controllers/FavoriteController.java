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
import zhedron.movie.dto.response.FavoriteResponse;
import zhedron.movie.dto.response.MessageResponse;
import zhedron.movie.dto.response.request.FavoriteRequest;
import zhedron.movie.entity.Favorite;
import zhedron.movie.mappers.FavoriteMapper;
import zhedron.movie.services.FavoriteService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/favorite")
@Tag(name = "Favorites", description = "Endpoints for managing user favorite lists and their media content")
public class FavoriteController {
    private final FavoriteService favoriteService;

    private final FavoriteMapper favoriteMapper;

    public FavoriteController(FavoriteService favoriteService, FavoriteMapper favoriteMapper) {
        this.favoriteService = favoriteService;
        this.favoriteMapper = favoriteMapper;
    }

    @PostMapping("/create")
    @Operation(
            summary = "Create a favorite list",
            description = "Creates a favorite list for the authenticated user. Favorite names must be unique for that user."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Favorite list created",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FavoriteResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "The request is invalid or a favorite with the same name already exists for the authenticated user",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MessageResponse.class),
                            examples = @ExampleObject(
                                    name = "Duplicate Favorite Name",
                                    value = "{\"message\": \"Favorite already exists\"}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<?> createFavorite(@Valid @RequestBody FavoriteRequest favoriteRequest, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            for (FieldError error : bindingResult.getFieldErrors()) {
                Map<String, String> errorMap = new HashMap<>();

                errorMap.put("message", error.getDefaultMessage());

                return new ResponseEntity<>(errorMap, HttpStatus.BAD_REQUEST);
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(favoriteService.create(favoriteRequest));
    }

    @PutMapping("/update/{id}")
    @Operation(summary = "Update a favorite list", description = "Updates the name of a favorite list owned by the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Favorite list updated", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = FavoriteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Favorite name is missing or blank", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MessageResponse.class), examples = @ExampleObject(value = "{\"message\": \"Name must not be empty\"}"))),
            @ApiResponse(responseCode = "404", description = "Favorite list not found or is not owned by the authenticated user", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MessageResponse.class), examples = @ExampleObject(value = "{\"message\": \"Favorite not found\"}"))),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<?> updateFavorite(@PathVariable long id, @Valid @RequestBody FavoriteRequest favoriteRequest, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            for (FieldError error : bindingResult.getFieldErrors()) {
                Map<String, String> errorMap = new HashMap<>();

                errorMap.put("message", error.getDefaultMessage());

                return new ResponseEntity<>(errorMap, HttpStatus.BAD_REQUEST);
            }
        }

        return ResponseEntity.ok(favoriteService.update(favoriteRequest, id));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a favorite list", description = "Retrieves a favorite list by ID for an authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Favorite list retrieved", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = FavoriteResponse.class))),
            @ApiResponse(responseCode = "404", description = "Favorite list not found", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MessageResponse.class), examples = @ExampleObject(value = "{\"message\": \"Favorite not found with 1\"}"))),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<FavoriteResponse> getFavorite(@PathVariable long id) {
        Favorite favoriteFound = favoriteService.findById(id);

        return ResponseEntity.ok(favoriteMapper.toFavoriteResponse(favoriteFound));
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "Delete a favorite list", description = "Deletes an owned favorite list.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Favorite list deleted", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MessageResponse.class), examples = @ExampleObject(value = "{\"message\": \"Favorite deleted successfully\"}"))),
            @ApiResponse(responseCode = "404", description = "Favorite list not found", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = "{\"message\": \"Favorite not found\"}"))),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<MessageResponse> deleteFavorite(@PathVariable long id) {
        favoriteService.deleteFavorite(id);

        return ResponseEntity.ok(new MessageResponse("Favorite deleted successfully"));
    }

    @PostMapping("/add-media/{favoriteId}/{mediaId}")
    @Operation(summary = "Add media to a favorite list", description = "Adds an existing media content item to an owned favorite list.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Media added", content = @Content(schema = @Schema(implementation = FavoriteResponse.class))),
            @ApiResponse(responseCode = "404", description = "Favorite or media content not found", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = {
                    @ExampleObject(
                            name = "Find favorite by id",
                            value = "{\"message\": \"Favorite not found\"}",
                            description = "Triggered if favorite not found"
                    ),
                    @ExampleObject(
                            name = "Find media by id",
                            value = "{\"message\": \"Media Content not found with 1\"}",
                            description = "Triggered if media not found"
                    )
            })),
            @ApiResponse(responseCode = "400", description = "Media content already exists in favorite", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(value = "{\"message\": \"Media Content exist in favorite\"}"))),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<FavoriteResponse>  addMediaContentToFavorite(@PathVariable long favoriteId, @PathVariable long mediaId) {
        return ResponseEntity.ok(favoriteService.addMediaContentToFavorite(favoriteId, mediaId));
    }

    @DeleteMapping("/remove-media/{favoriteId}/{mediaId}")
    @Operation(summary = "Remove media from a favorite list", description = "Removes an existing media content item from an owned favorite list.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Media removed", content = @Content(schema = @Schema(implementation = FavoriteResponse.class))),
            @ApiResponse(responseCode = "404", description = "Favorite or media content association not found", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = {
                    @ExampleObject(
                            name = "Find favorite by id",
                            value = "{\"message\": \"Favorite not found\"}",
                            description = "Triggered if favorite not found"
                    ),
                    @ExampleObject(
                            name = "Find media by id",
                            value = "{\"message\": \"Media Content not found with 1\"}",
                            description = "Triggered if media not found"
                    )
            })),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<FavoriteResponse> removeMediaContentFromFavorite(@PathVariable long favoriteId, @PathVariable long mediaId) {
        return ResponseEntity.ok(favoriteService.removeMediaContentFromFavorite(favoriteId, mediaId));
    }

    @GetMapping("/name")
    @Operation(
            summary = "Get a favorite list by name",
            description = "Retrieves a favorite list with the requested name belonging to the currently authenticated user."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Favorite list retrieved",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FavoriteResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Favorite list with the requested name was not found for the current user",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Favorite Not Found",
                                    value = "{\"message\": \"Favorite not found with Watch later\"}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public FavoriteResponse getFavoriteByName(@RequestParam String name) {
        return favoriteService.getFavoriteByName(name);
    }
}
