package com.ethicalsoft.ethicalsoft_complience.adapters.out.auth;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.User;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.auth.*;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.AuthProviderEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.UserRoleEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.UserRepository;
import com.ethicalsoft.ethicalsoft_complience.application.port.auth.AuthCommandPort;
import com.ethicalsoft.ethicalsoft_complience.application.port.auth.RefreshTokenPort;
import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import com.ethicalsoft.ethicalsoft_complience.exception.UserNotFoundException;
import com.ethicalsoft.ethicalsoft_complience.infra.security.TokenService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthCommandAdapter implements AuthCommandPort {

    private final AuthAdapter authService;
    private final TokenService tokenService;
    private final RefreshTokenPort refreshTokenPort;
    private final UserRepository userRepository;

    @Value("${google.client-id}")
    private String googleClientId;

    @Override
    public AuthDTO token(LoginDTO loginDTO) {
        var authentication = authService.token(loginDTO);
        var user = (User) authentication.getPrincipal();
        var accessToken = tokenService.generateToken(user);
        var refreshToken = refreshTokenPort.createRefreshToken(user);
        return new AuthDTO(accessToken, refreshToken);
    }

    @Override
    public AuthDTO refresh(RefreshTokenDTO refreshTokenDTO) {
        var email = refreshTokenPort.validateRefreshToken(refreshTokenDTO);
        var user = (User) authService.loadUserByUsername(email);
        var accessToken = tokenService.generateToken(user);
        var newRefresh = refreshTokenPort.createRefreshToken(user);
        return new AuthDTO(accessToken, newRefresh);
    }

    @Override
    public void logout(RefreshTokenDTO refreshTokenDTO) {
        refreshTokenPort.deleteRefreshToken(refreshTokenDTO);
    }

    @Override
    public void register(RegisterUserDTO registerUserDTO) {
        authService.register(registerUserDTO);
    }

    @Override
    @Transactional
    public AuthDTO googleAuth(GoogleAuthDTO googleAuthDTO) {
        log.info("[google-auth] Iniciando autenticação via Google");
        GoogleIdToken.Payload payload = verifyGoogleToken(googleAuthDTO.getIdToken());

        String email = payload.getEmail();
        String googleId = payload.getSubject();
        String firstName = (String) payload.get("given_name");
        String lastName = (String) payload.get("family_name");
        String avatarUrl = (String) payload.get("picture");

        User user = userRepository.findByEmail(email)
                .map(existing -> linkGoogleAccount(existing, googleId, avatarUrl))
                .orElseGet(() -> createGoogleUser(email, firstName, lastName, googleId, avatarUrl));

        log.info("[google-auth] Usuário {} autenticado via Google", email);
        var accessToken = tokenService.generateToken(user);
        var refreshToken = refreshTokenPort.createRefreshToken(user);
        return new AuthDTO(accessToken, refreshToken);
    }

    @Override
    @Transactional
    public void acceptTerms(AcceptTermsDTO acceptTermsDTO) {
        log.info("[accept-terms] Aceitando termos para email={}", acceptTermsDTO.getEmail());
        User user = userRepository.findByEmail(acceptTermsDTO.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.setAcceptedTerms(true);
        user.setFirstAccess(false);
        userRepository.save(user);
        log.info("[accept-terms] Termos aceitos para usuário id={}", user.getId());
    }

    @Override
    public ExtendSessionResponseDTO extendSession(ExtendSessionDTO extendSessionDTO) {
        log.info("[extend-session] Estendendo sessão do usuário");
        String email = refreshTokenPort.validateRefreshToken(new RefreshTokenDTO(extendSessionDTO.getRefreshToken()));
        var user = (User) authService.loadUserByUsername(email);
        String newAccessToken = tokenService.generateToken(user);
        Long newExpirationTime = refreshTokenPort.extendRefreshTokenExpiry(extendSessionDTO.getRefreshToken());
        log.info("[extend-session] Sessão estendida. Nova hora de expiração: {}", newExpirationTime);
        return new ExtendSessionResponseDTO(newExpirationTime, newAccessToken);
    }

    private GoogleIdToken.Payload verifyGoogleToken(String idToken) {
        try {
            var transport = new NetHttpTransport();
            var jsonFactory = GsonFactory.getDefaultInstance();
            var verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();
            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) {
                throw new BusinessException("Invalid Google ID token");
            }
            return token.getPayload();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[google-auth] Falha ao verificar token Google", e);
            throw new BusinessException("Failed to verify Google token");
        }
    }

    private User linkGoogleAccount(User user, String googleId, String avatarUrl) {
        if (user.getGoogleId() == null) {
            user.setGoogleId(googleId);
        }
        if (avatarUrl != null && user.getAvatarUrl() == null) {
            user.setAvatarUrl(avatarUrl);
        }
        user.setAuthProvider(AuthProviderEnum.GOOGLE);
        return userRepository.save(user);
    }

    private User createGoogleUser(String email, String firstName, String lastName, String googleId, String avatarUrl) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName(firstName != null ? firstName : email.split("@")[0]);
        user.setLastName(lastName != null ? lastName : "");
        user.setPassword(null);
        user.setGoogleId(googleId);
        user.setAvatarUrl(avatarUrl);
        user.setRole(UserRoleEnum.ADMIN);
        user.setFirstAccess(true);
        user.setAcceptedTerms(false);
        user.setAuthProvider(AuthProviderEnum.GOOGLE);
        return userRepository.save(user);
    }
}
