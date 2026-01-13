package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.auth;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.RefreshToken;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.User;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.auth.RefreshTokenDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.RefreshTokenRepository;
import com.ethicalsoft.ethicalsoft_complience.application.port.RefreshTokenPort;
import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenAdapter implements RefreshTokenPort {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${api.security.token.refresh.expiration}")
    private Long refreshTokenDuration;

    @Override
    public String createRefreshToken(User user) {
        try {
            log.info("[refresh-token] Gerando refresh token para usuário id={}", user != null ? user.getId() : null);
            String token = UUID.randomUUID().toString();
            String hashedToken = DigestUtils.sha256Hex(token);
            Instant expiryDate = Instant.now().plusMillis(refreshTokenDuration);

            Optional<RefreshToken> existingTokenOpt = refreshTokenRepository.findByUser(user);

            RefreshToken refreshTokenToSave;
            if (existingTokenOpt.isPresent()) {
                refreshTokenToSave = existingTokenOpt.get();
                refreshTokenToSave.setToken(hashedToken);
                refreshTokenToSave.setExpiryDate(expiryDate);
            } else {
                refreshTokenToSave = new RefreshToken(hashedToken, user, expiryDate);
            }

            refreshTokenRepository.save(refreshTokenToSave);
            return token;
        } catch (Exception ex) {
            log.error("[refresh-token] Falha ao criar refresh token para usuário id={}", user != null ? user.getId() : null, ex);
            throw ex;
        }
    }

    @Override
    public String validateRefreshToken(RefreshTokenDTO refreshTokenDTO) {
        return validateRefreshToken(refreshTokenDTO != null ? refreshTokenDTO.getRefreshToken() : null);
    }

    private String validateRefreshToken(String token) {
        try {
            log.info("[refresh-token] Validando refresh token recebido");
            String hashedToken = DigestUtils.sha256Hex(token);
            var refreshTokenOpt = refreshTokenRepository.findByToken(hashedToken);
            if (refreshTokenOpt.isEmpty()) {
                throw new BusinessException("Refresh token not found or invalid.");
            }

            RefreshToken refreshToken = refreshTokenOpt.get();
            if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
                refreshTokenRepository.delete(refreshToken);
                throw new BusinessException("Refresh token expired.");
            }
            return refreshToken.getUser().getEmail();
        } catch (Exception ex) {
            log.error("[refresh-token] Falha ao validar refresh token", ex);
            throw ex;
        }
    }

    @Override
    @Transactional
    public void deleteRefreshToken(RefreshTokenDTO refreshTokenDTO) {
        try {
            log.info("[refresh-token] Removendo refresh token informado");
            if (refreshTokenDTO == null || refreshTokenDTO.getRefreshToken() == null) {
                return;
            }
            String hashedToken = DigestUtils.sha256Hex(refreshTokenDTO.getRefreshToken());
            Optional<RefreshToken> tokenOptional = refreshTokenRepository.findByToken(hashedToken);
            tokenOptional.ifPresent(token -> refreshTokenRepository.deleteById(token.getId()));
        } catch (Exception ex) {
            log.error("[refresh-token] Falha ao deletar refresh token", ex);
            throw ex;
        }
    }
}

