package zhedron.movie.services.impl;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import zhedron.movie.dto.response.UserResponse;
import zhedron.movie.dto.response.request.ResetPasswordRequest;
import zhedron.movie.entity.User;
import zhedron.movie.exceptions.CodeVerifiedException;
import zhedron.movie.exceptions.SamePasswordException;
import zhedron.movie.exceptions.TokenInvalidException;
import zhedron.movie.exceptions.UserNotFoundException;
import zhedron.movie.mappers.UserMapper;
import zhedron.movie.repository.UserRepository;
import zhedron.movie.services.ResetPasswordService;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Service
public class ResetPasswordServiceImpl implements ResetPasswordService {
    private final StringRedisTemplate redis;

    private final String RESET_PREFIX = "password:reset:";
    private final String CODE_PREFIX = "code:";
    private final String VERIFIED_PREFIX = "verified:";

    private final UserRepository userRepository;

    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder;

    public ResetPasswordServiceImpl(StringRedisTemplate redis, UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.redis = redis;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Map<String, Object> createToken(String email) {
        if (!userRepository.existsByEmail(email)) {
            throw new UserNotFoundException("User not found with email: " + email);
        }

        String token = UUID.randomUUID().toString();

        int code = generateCode();

        Map<String, Object> map = new HashMap<>();

        map.put("token", token);
        map.put("code", code);

        redis.opsForValue().set(RESET_PREFIX + token, email, Duration.ofMinutes(15));
        redis.opsForValue().set(CODE_PREFIX + email, String.valueOf(code), Duration.ofMinutes(15));

        return map;
    }

    @Override
    public boolean findByToken(String token) {
        return redis.hasKey(RESET_PREFIX + token);
    }

    @Override
    public UserResponse resetPassword(String token, ResetPasswordRequest request) {
        String email = redis.opsForValue().get(RESET_PREFIX + token);
        boolean verified = redis.opsForValue().get(VERIFIED_PREFIX + token) != null && Boolean.parseBoolean(redis.opsForValue().get(VERIFIED_PREFIX + token));

        if (email == null) {
            throw new TokenInvalidException("Invalid token");
        } else if (!verified) {
            throw new CodeVerifiedException("The code not verified");
        }

        User user = userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

        String newPassword = request.getPassword();

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new SamePasswordException("You're use the same password");
        }

        user.setPassword(passwordEncoder.encode(newPassword));

        redis.delete(RESET_PREFIX + token);
        redis.delete(CODE_PREFIX);
        redis.delete(VERIFIED_PREFIX + token);

        User changedPasswordUser = userRepository.save(user);

        return userMapper.toUserDTO(changedPasswordUser);
    }

    @Override
    public boolean verifyCode(String code, String token) {
        boolean tokenValid = redis.hasKey(RESET_PREFIX + token);

        String email = redis.opsForValue().get(RESET_PREFIX + token);

        if (email == null) {
            return false;
        }

        boolean codeValid = redis.opsForValue().get(CODE_PREFIX + email) != null && redis.opsForValue().get(CODE_PREFIX + email).equals(code);

        boolean valid = tokenValid && codeValid;

        if (valid) {
            redis.opsForValue().set(VERIFIED_PREFIX + token, "true", Duration.ofMinutes(15));

            return true;
        } else {
            return false;
        }
    }

    private int generateCode() {
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < 4; i++) {
            Random random = new Random();

            int result = random.nextInt(0, 9);

            code.append(String.valueOf(result));
        }

        return Integer.parseInt(code.toString());
    }
}
