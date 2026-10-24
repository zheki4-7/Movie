package zhedron.movie.services;

import zhedron.movie.dto.response.UserResponse;
import zhedron.movie.dto.response.request.ResetPasswordRequest;

import java.util.Map;

public interface ResetPasswordService {
    Map<String, Object> createToken(String email);

    boolean findByToken(String token);

    UserResponse resetPassword(String token, ResetPasswordRequest request);

    boolean verifyCode(String code, String token);
}
