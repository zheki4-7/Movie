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
import zhedron.movie.dto.response.UserResponse;
import zhedron.movie.dto.response.request.UserRegistrationRequest;
import zhedron.movie.enums.Role;
import zhedron.movie.services.UserService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@Tag(name = "Users", description = "Endpoints for user account management and registration")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/registration")
    @Operation(
            summary = "Register a new user",
            description = "Registers a new user account with an encrypted password."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "User registered successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request — Validation failure or email already in use",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "object", implementation = Map.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Duplicate Email Error",
                                            summary = "When the email is already registered",
                                            value = "{\"message\": \"Email already exists\"}"
                                    ),
                                    @ExampleObject(
                                            name = "Validation Error",
                                            summary = "When required fields fail validation",
                                            value = "{\"message\": \"Email must not be empty\"}"
                                    )
                            }
                    )
            )
    })
    public ResponseEntity<?> createUser(@Valid @RequestBody UserRegistrationRequest userRegistrationRequest, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            for (FieldError error : bindingResult.getFieldErrors()) {
                Map<String, String> errors = new HashMap<>();

                errors.put("message", error.getDefaultMessage());

                return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(userRegistrationRequest));
    }

    @PutMapping("/change-role/{id}")
    @Operation(
            summary = "Change user role",
            description = "Updates the authority role (e.g., ADMIN, USER) for a specified user account."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User role updated successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "User Not Found",
                                    value = "{\"message\": \"User not found with 1\"}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<UserResponse> changeRole(@PathVariable long id, @RequestBody Role role) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.changeRole(id, role));
    }
}
