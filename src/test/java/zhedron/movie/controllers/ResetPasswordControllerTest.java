package zhedron.movie.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import zhedron.movie.config.SecurityConfig;
import zhedron.movie.dto.response.UserResponse;
import zhedron.movie.dto.response.request.ResetPasswordRequest;
import zhedron.movie.enums.Role;
import zhedron.movie.repository.UserRepository;
import zhedron.movie.services.ResetPasswordService;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResetPasswordController.class)
@Import({SecurityConfig.class, ControllerSecurityTestConfig.class})
class ResetPasswordControllerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResetPasswordService resetPasswordService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void createTokenReturnsTokenData() throws Exception {
        when(resetPasswordService.createToken("neo@example.com"))
                .thenReturn(Map.of("token", "reset-token", "code", 1234));

        mockMvc.perform(post("/api/reset/create").param("email", "neo@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("reset-token"))
                .andExpect(jsonPath("$.code").value(1234));

        verify(resetPasswordService).createToken("neo@example.com");
    }

    @Test
    void findByTokenReturnsSuccessMessageWhenTokenExists() throws Exception {
        when(resetPasswordService.findByToken("reset-token")).thenReturn(true);

        mockMvc.perform(get("/api/reset").param("token", "reset-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Token reset-token found successfully"));
    }

    @Test
    void findByTokenReturnsNotFoundWhenTokenDoesNotExist() throws Exception {
        when(resetPasswordService.findByToken("missing-token")).thenReturn(false);

        mockMvc.perform(get("/api/reset").param("token", "missing-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Token missing-token not found"));
    }

    @Test
    void resetPasswordReturnsUpdatedUser() throws Exception {
        ResetPasswordRequest request = passwordRequest("new-password");
        UserResponse response = new UserResponse(1L, "neo", "neo@example.com", null, null, Role.USER);
        when(resetPasswordService.resetPassword(eq("reset-token"), any(ResetPasswordRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/reset/change-password")
                        .param("token", "reset-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("neo@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));

        verify(resetPasswordService).resetPassword(eq("reset-token"), any(ResetPasswordRequest.class));
    }

    @Test
    void resetPasswordReturnsValidationMessageForBlankPassword() throws Exception {
        mockMvc.perform(put("/api/reset/change-password")
                        .param("token", "reset-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(passwordRequest(""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password must not be empty"));

        verify(resetPasswordService, never()).resetPassword(any(), any(ResetPasswordRequest.class));
    }

    @Test
    void verifyCodeReturnsSuccessMessage() throws Exception {
        when(resetPasswordService.verifyCode("1234", "reset-token")).thenReturn(true);

        mockMvc.perform(post("/api/reset/verify")
                        .param("code", "1234")
                        .param("token", "reset-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Code verified successfully"));

        verify(resetPasswordService).verifyCode("1234", "reset-token");
    }

    @Test
    void endpointsRejectRequestsWithMissingRequiredParameters() throws Exception {
        mockMvc.perform(post("/api/reset/create"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/reset"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/reset/verify").param("code", "1234"))
                .andExpect(status().isBadRequest());

        verify(resetPasswordService, never()).createToken(any());
        verify(resetPasswordService, never()).findByToken(any());
        verify(resetPasswordService, never()).verifyCode(any(), any());
    }

    private static ResetPasswordRequest passwordRequest(String password) {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setPassword(password);
        return request;
    }
}
