package zhedron.movie.services.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import zhedron.movie.dto.response.UserResponse;
import zhedron.movie.dto.response.request.ResetPasswordRequest;
import zhedron.movie.entity.User;
import zhedron.movie.exceptions.CodeVerifiedException;
import zhedron.movie.exceptions.SamePasswordException;
import zhedron.movie.exceptions.TokenInvalidException;
import zhedron.movie.exceptions.UserNotFoundException;
import zhedron.movie.mappers.UserMapper;
import zhedron.movie.repository.UserRepository;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResetPasswordServiceImplTest {
    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private ResetPasswordServiceImpl resetPasswordService;

    @Test
    void createTokenStoresResetDataAndReturnsTokenAndCode() {
        when(userRepository.existsByEmail("neo@example.com")).thenReturn(true);
        when(redis.opsForValue()).thenReturn(valueOperations);

        Map<String, Object> result = resetPasswordService.createToken("neo@example.com");

        assertTrue(result.get("token") instanceof String);
        assertTrue(result.get("code") instanceof Integer);
        verify(valueOperations).set(eq("password:reset:" + result.get("token")), eq("neo@example.com"), eq(Duration.ofMinutes(15)));
        verify(valueOperations).set(eq("code:neo@example.com"), eq(String.valueOf(result.get("code"))), eq(Duration.ofMinutes(15)));
    }

    @Test
    void createTokenThrowsWhenEmailDoesNotExist() {
        when(userRepository.existsByEmail("missing@example.com")).thenReturn(false);

        assertThrows(UserNotFoundException.class, () -> resetPasswordService.createToken("missing@example.com"));

        verify(redis, never()).opsForValue();
    }

    @Test
    void findByTokenChecksRedisKey() {
        when(redis.hasKey("password:reset:reset-token")).thenReturn(true);

        assertTrue(resetPasswordService.findByToken("reset-token"));

        verify(redis).hasKey("password:reset:reset-token");
    }

    @Test
    void resetPasswordUpdatesPasswordAndDeletesResetData() {
        User user = user("neo@example.com", "old-hash");
        User changedUser = user("neo@example.com", "new-hash");
        UserResponse expectedResponse = new UserResponse(1L, "neo", "neo@example.com", null, null, null);
        ResetPasswordRequest request = passwordRequest("new-password");
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("password:reset:reset-token")).thenReturn("neo@example.com");
        when(valueOperations.get("verified:reset-token")).thenReturn("true");
        when(userRepository.findByEmail("neo@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("new-password", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");
        when(userRepository.save(user)).thenReturn(changedUser);
        when(userMapper.toUserDTO(changedUser)).thenReturn(expectedResponse);

        UserResponse result = resetPasswordService.resetPassword("reset-token", request);

        assertSame(expectedResponse, result);
        assertEquals("new-hash", user.getPassword());
        verify(redis).delete("password:reset:reset-token");
        verify(redis).delete("code:");
        verify(redis).delete("verified:reset-token");
        verify(userRepository).save(user);
    }

    @Test
    void resetPasswordThrowsForInvalidToken() {
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("password:reset:reset-token")).thenReturn(null);

        assertThrows(TokenInvalidException.class,
                () -> resetPasswordService.resetPassword("reset-token", passwordRequest("new-password")));

        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void resetPasswordThrowsWhenCodeIsNotVerified() {
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("password:reset:reset-token")).thenReturn("neo@example.com");
        when(valueOperations.get("verified:reset-token")).thenReturn("false");

        assertThrows(CodeVerifiedException.class,
                () -> resetPasswordService.resetPassword("reset-token", passwordRequest("new-password")));

        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void resetPasswordThrowsWhenUserIsMissing() {
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("password:reset:reset-token")).thenReturn("missing@example.com");
        when(valueOperations.get("verified:reset-token")).thenReturn("true");
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> resetPasswordService.resetPassword("reset-token", passwordRequest("new-password")));
    }

    @Test
    void resetPasswordThrowsWhenNewPasswordMatchesCurrentPassword() {
        User user = user("neo@example.com", "same-hash");
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("password:reset:reset-token")).thenReturn("neo@example.com");
        when(valueOperations.get("verified:reset-token")).thenReturn("true");
        when(userRepository.findByEmail("neo@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("same-password", "same-hash")).thenReturn(true);

        assertThrows(SamePasswordException.class,
                () -> resetPasswordService.resetPassword("reset-token", passwordRequest("same-password")));

        verify(userRepository, never()).save(any());
    }

    @Test
    void verifyCodeReturnsFalseForMissingToken() {
        when(redis.hasKey("password:reset:missing-token")).thenReturn(false);
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("password:reset:missing-token")).thenReturn(null);

        assertFalse(resetPasswordService.verifyCode("1234", "missing-token"));
    }

    @Test
    void verifyCodeReturnsFalseForWrongCode() {
        when(redis.hasKey("password:reset:reset-token")).thenReturn(true);
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("password:reset:reset-token")).thenReturn("neo@example.com");
        when(valueOperations.get("code:neo@example.com")).thenReturn("4321");

        assertFalse(resetPasswordService.verifyCode("1234", "reset-token"));
    }

    @Test
    void verifyCodeMarksTokenAsVerifiedForCorrectCode() {
        when(redis.hasKey("password:reset:reset-token")).thenReturn(true);
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("password:reset:reset-token")).thenReturn("neo@example.com");
        when(valueOperations.get("code:neo@example.com")).thenReturn("1234");

        assertTrue(resetPasswordService.verifyCode("1234", "reset-token"));

        verify(valueOperations).set("verified:reset-token", "true", Duration.ofMinutes(15));
    }

    private static ResetPasswordRequest passwordRequest(String password) {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setPassword(password);
        return request;
    }

    private static User user(String email, String password) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(password);
        return user;
    }
}
