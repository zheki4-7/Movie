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
import zhedron.movie.dto.response.CommentResponse;
import zhedron.movie.dto.response.MessageResponse;
import zhedron.movie.dto.response.request.CommentRequest;
import zhedron.movie.services.CommentService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/comment")
@Tag(name = "Comments", description = "Endpoints for managing comments on media content")
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/{mediaContentId}")
    @Operation(
            summary = "Create a comment",
            description = "Adds a new comment to the specified media content for the currently authenticated user."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Comment created successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CommentResponse.class)
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
                                    value = "{\"message\": \"Write your comment, text must not be empty\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Media content not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "object", implementation = Map.class),
                            examples = @ExampleObject(
                                    name = "Media Not Found",
                                    value = "{\"message\": \"Media content not found with 42\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied - User is not the author of the comment",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Forbidden",
                                    value = "{\"message\": \"You are not allowed to edit this comment\"}"
                            )
                    )
            )
    })
    public ResponseEntity<?> createComment(@Valid @RequestBody CommentRequest commentRequest, BindingResult bindingResult, @PathVariable long mediaContentId) {
        if (bindingResult.hasErrors()) {
            for (FieldError error : bindingResult.getFieldErrors()) {
                Map<String, String> errors = new HashMap<>();

                errors.put("message", error.getDefaultMessage());

                return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
            }
        }

        return new ResponseEntity<>(commentService.addComment(commentRequest, mediaContentId), HttpStatus.CREATED);
    }

    @GetMapping("/{id}/{mediaContentId}")
    @Operation(
            summary = "Get comment by ID and Media Content ID",
            description = "Retrieves a comment associated with a specific media content item."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Comment retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CommentResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Comment or Media Content not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "Comment Not Found",
                                            value = "{\"message\": \"Comment not found with 1\"}"
                                    ),
                                    @ExampleObject(
                                            name = "Comment Not In Media",
                                            value = "{\"message\": \"Comment not found in Media with 1\"}"
                                    )
                            }
                    )
            )
    })
    public ResponseEntity<CommentResponse> findById(@PathVariable long id, @PathVariable long mediaContentId) {
        return new ResponseEntity<>(commentService.findById(id, mediaContentId), HttpStatus.OK);
    }

    @DeleteMapping("/delete/{id}")
    @Operation(
            summary = "Delete a comment",
            description = "Deletes a comment by its ID. Only the user who created the comment is allowed to delete it."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Comment deleted successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MessageResponse.class),
                            examples = @ExampleObject(
                                    name = "Success Message",
                                    value = "{\"message\": \"Comment deleted successfully\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied — User is not the author of the comment",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Forbidden",
                                    value = "{\"message\": \"You are not allowed to delete this comment\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Comment not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Not Found",
                                    value = "{\"message\": \"Comment not found with 1\"}"
                            )
                    )
            )
    })
    public ResponseEntity<MessageResponse> deleteComment(@PathVariable long id) {
        commentService.deleteComment(id);

        return new ResponseEntity<>(new MessageResponse("Comment deleted successfully"), HttpStatus.OK);
    }

    @PutMapping("/edit/{id}")
    @Operation(
            summary = "Edit a comment",
            description = "Updates the text of an existing comment and sets the 'edited' flag to true."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Comment updated successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CommentResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error in request payload",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Validation Error",
                                    value = "{\"message\": \"Write your comment, text must not be empty\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Comment not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Not Found",
                                    value = "{\"message\": \"Comment not found with 1\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied - User is not the author of the comment",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Forbidden",
                                    value = "{\"message\": \"You are not allowed to edit this comment\"}"
                            )
                    )
            )
    })
    public ResponseEntity<?> editComment(@RequestBody @Valid CommentRequest commentRequest, BindingResult bindingResult, @PathVariable long id) {
        if (bindingResult.hasErrors()) {
            for (FieldError error : bindingResult.getFieldErrors()) {
                Map<String, String>  errors = new HashMap<>();

                errors.put("message", error.getDefaultMessage());

                return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
            }
        }

        return ResponseEntity.ok(commentService.editComment(commentRequest, id));
    }
}
