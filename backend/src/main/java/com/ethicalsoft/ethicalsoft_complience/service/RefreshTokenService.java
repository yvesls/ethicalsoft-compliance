package com.ethicalsoft.ethicalsoft_complience.service;

import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import com.ethicalsoft.ethicalsoft_complience.model.RefreshToken;
import com.ethicalsoft.ethicalsoft_complience.model.User;
import com.ethicalsoft.ethicalsoft_complience.model.dto.request.RefreshTokenRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class RefreshTokenService {

    @Value("${api.security.token.refresh.expiration}")
    private Long refreshTokenDuration;

    private final RefreshTokenRepository refreshTokenRepository;

    public String createRefreshToken(User user) {
        String token = UUID.randomUUID().toString();
        Instant expiryDate = Instant.now().plusMillis(refreshTokenDuration);
        RefreshToken refreshToken = new RefreshToken(token, user, expiryDate);
        refreshTokenRepository.deleteByUser(user);
        refreshTokenRepository.save(refreshToken);
        return token;
    }

    public String validateRefreshToken(String token) {
        var refreshTokenOpt = refreshTokenRepository.findByToken(token);
        if (refreshTokenOpt.isEmpty()) {
            return null;
        }
        RefreshToken refreshToken = refreshTokenOpt.get();
        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new BusinessException("Refresh token expired");
        }
        return refreshToken.getUser().getEmail();
    }

    public User getUserFromRefreshToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .map(RefreshToken::getUser)
                .orElseThrow(() -> new BusinessException("Invalid refresh token"));
    }

    @Transactional
    public void deleteRefreshToken(RefreshTokenRequestDTO refreshTokenRequestDTO) {
        refreshTokenRepository.deleteByToken(refreshTokenRequestDTO.getRefreshToken());
    }
}