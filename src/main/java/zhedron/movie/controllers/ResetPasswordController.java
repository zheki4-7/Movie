package zhedron.movie.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import zhedron.movie.dto.response.MessageResponse;
import zhedron.movie.dto.response.UserResponse;
import zhedron.movie.dto.response.request.ResetPasswordRequest;
import zhedron.movie.services.ResetPasswordService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reset")
public class ResetPasswordController {
    private final ResetPasswordService resetPasswordService;

    public ResetPasswordController(ResetPasswordService resetPasswordService) {
        this.resetPasswordService = resetPasswordService;
    }

    @PostMapping("/create")
    @Operation(summary = "Create a password reset token", description = "Creates a password reset token and verification code for an existing user email.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reset token created", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "User with the supplied email was not found", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<Map<String, Object>> createToken(@RequestParam String email) {
        return ResponseEntity.ok(resetPasswordService.createToken(email));
    }

    @GetMapping
    @Operation(summary = "Check a password reset token", description = "Checks whether a password reset token exists and is still valid.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token exists", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "404", description = "Token does not exist", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<MessageResponse> findByToken(@RequestParam String token) {
        if (resetPasswordService.findByToken(token)) {
            return ResponseEntity.ok(new MessageResponse("Token " + token + " found successfully"));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("Token " + token + " not found"));
        }
    }

    @PutMapping("/change-password")
    @Operation(summary = "Reset a user password", description = "Changes the password after the reset token and verification code have been validated.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password changed", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Reset code has not been verified or the new password matches the current password", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "404", description = "Reset token or user was not found", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest resetPasswordRequest, BindingResult bindingResult, @RequestParam String token) {
        if (bindingResult.hasErrors()) {
            for (FieldError error : bindingResult.getFieldErrors()) {
                Map<String, String> errorMap = new HashMap<>();

                errorMap.put("message",  error.getDefaultMessage());

                return new ResponseEntity<>(errorMap, HttpStatus.BAD_REQUEST);
            }
        }

        return ResponseEntity.ok(resetPasswordService.resetPassword(token, resetPasswordRequest));
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify a password reset code", description = "Verifies the code associated with a password reset token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Code verified", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Code or token is invalid", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<MessageResponse> verifyCode(@RequestParam String code, @RequestParam String token) {
        resetPasswordService.verifyCode(code, token);

        return ResponseEntity.ok(new MessageResponse("Code verified successfully"));
    }
}
