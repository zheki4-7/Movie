package zhedron.movie.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import zhedron.movie.dto.response.MessageResponse;
import zhedron.movie.dto.response.TokenResponse;
import zhedron.movie.dto.response.request.LoginRequest;
import zhedron.movie.entity.RefreshToken;
import zhedron.movie.entity.User;
import zhedron.movie.exceptions.RefreshTokenNotFoundException;
import zhedron.movie.services.JwtService;
import zhedron.movie.services.RefreshTokenService;
import zhedron.movie.services.UserService;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Authentication", description = "Endpoints for user authentication and JWT session management")
public class AuthController {
    private final JwtService jwtService;
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;

    public AuthController(JwtService jwtService, UserService userService, RefreshTokenService refreshTokenService, AuthenticationManager authenticationManager) {
        this.jwtService = jwtService;
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/login")
    @Operation(
            summary = "User Login",
            description = "Authenticates user credentials. On success, returns a JWT token in the response body and sets an HttpOnly 'accessToken' cookie."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully authenticated",
                    headers = @Header(
                            name = HttpHeaders.SET_COOKIE,
                            description = "HttpOnly cookie containing the JWT token",
                            schema = @Schema(type = "string", example = "accessToken=eyJhbGci...; Path=/; Max-Age=3600; HttpOnly")
                    ),
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Successful Response",
                                    value = "{\"token\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error in request body",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "Missing Email Error",
                                            summary = "When email field validation fails",
                                            value = "{\"message\": \"Write your email, email must not be empty\"}"
                                    ),
                                    @ExampleObject(
                                            name = "Missing Password Error",
                                            summary = "When password field validation fails",
                                            value = "{\"message\": \"Write your password, password must not be empty\"}"
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials provided",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MessageResponse.class),
                            examples = @ExampleObject(
                                    name = "Bad Credentials",
                                    value = "{\"message\": \"Invalid email or password\"}"
                            )
                    )
            )
    })
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            for (FieldError error : bindingResult.getFieldErrors()) {
                Map<String, Object> errors = new HashMap<>();

                errors.put("message", error.getDefaultMessage());

                return ResponseEntity.badRequest().body(errors);
            }
        }

            try {
                Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(), loginRequest.getPassword()
                ));

                if (authentication.isAuthenticated()) {
                    User user = userService.findByEmail(loginRequest.getEmail());

                    String accessToken = jwtService.generateToken(user);
                    String refreshToken = refreshTokenService.generateRefreshToken(user.getEmail());

                    ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", accessToken)
                            .httpOnly(true)
                            .maxAge(Duration.ofHours(1))
                            .path("/")
                            .build();

                    ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", refreshToken)
                            .httpOnly(true)
                            .path("/api/refreshtoken")
                            .maxAge(Duration.ofDays(7))
                            .build();

                    return ResponseEntity.status(HttpStatus.OK)
                            .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                            .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                            .body(new TokenResponse(accessToken, refreshToken));
                }
            } catch (BadCredentialsException e) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("Invalid email or password"));
            }
        return null;
    }

    @PostMapping("/refreshtoken")
    @Operation(
            summary = "Refresh Tokens",
            description = "Extracts the Refresh token from the request cookie. If valid, generates and returns a new token pair in both response body and cookies."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Tokens refreshed successfully",
                    headers = {
                            @Header(
                                    name = HttpHeaders.SET_COOKIE,
                                    description = "Updated HttpOnly cookies for access token and refresh token",
                                    schema = @Schema(type = "string", example = "accessToken=eyJhbGci...; Path=/; Max-Age=3600; HttpOnly")
                            )
                    },
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TokenResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Refresh token missing from Cookie or not found in database",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "Token Not Found",
                                            summary = "Token does not exist in database",
                                            value = "{\"message\": \"Refresh token not found\"}"
                                    )
                            }
                    )
            )
    })
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        String token = null;

        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("refreshToken")) {
                    token = cookie.getValue();
                }
            }
        }

        if (token != null) {
           return refreshTokenService.findByRefreshToken(token)
                   .map(refreshTokenService::validateToken)
                   .map(RefreshToken::getUser)
                   .map(user -> {
                      String accessToken = jwtService.generateToken(user);
                      String refreshToken = refreshTokenService.generateRefreshToken(user.getEmail());

                      ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", accessToken)
                              .httpOnly(true)
                              .maxAge(Duration.ofHours(1))
                              .path("/")
                              .build();

                      ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", refreshToken)
                              .httpOnly(true)
                              .maxAge(Duration.ofDays(7))
                              .path("/api/refreshtoken")
                              .build();

                      return ResponseEntity.status(HttpStatus.OK)
                              .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                              .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                              .body(new TokenResponse(accessToken, refreshToken));
                   }).orElseThrow(() -> new RefreshTokenNotFoundException("Refresh token not found"));
        }

        return ResponseEntity.notFound().build();
    }
}
