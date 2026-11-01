package zhedron.movie.services.impl;

import org.springframework.stereotype.Service;
import zhedron.movie.entity.RefreshToken;
import zhedron.movie.entity.User;
import zhedron.movie.exceptions.RefreshTokenExpiredException;
import zhedron.movie.repository.RefreshTokenRepository;
import zhedron.movie.repository.UserRepository;
import zhedron.movie.services.RefreshTokenService;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository, UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    @Override
    public String generateRefreshToken(String email) {
        RefreshToken refreshToken = new RefreshToken();

        Optional<User> user = userRepository.findByEmail(email);

        if (user.isPresent()) {
            refreshToken.setUser(user.get());
            refreshToken.setExpiredAt(LocalDateTime.now().plusWeeks(1));
            refreshToken.setRefreshToken(UUID.randomUUID().toString());

            RefreshToken savedRefreshToken = refreshTokenRepository.save(refreshToken);

            return savedRefreshToken.getRefreshToken();
        } else {
            return null;
        }
    }

    @Override
    public RefreshToken validateToken(RefreshToken refreshToken) {
        if (refreshToken.getExpiredAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(refreshToken);

            throw new RefreshTokenExpiredException("Refresh token has expired");
        }

        return refreshToken;
    }

    @Override
    public Optional<RefreshToken> findByRefreshToken(String refreshToken) {
        return refreshTokenRepository.findByRefreshToken(refreshToken);
    }
}
