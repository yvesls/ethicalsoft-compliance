package com.ethicalsoft.ethicalsoft_complience.adapters.out.auth;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.User;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.auth.AuthDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.auth.LoginDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.auth.RefreshTokenDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.auth.RegisterUserDTO;
import com.ethicalsoft.ethicalsoft_complience.application.port.AuthCommandPort;
import com.ethicalsoft.ethicalsoft_complience.application.port.RefreshTokenPort;
import com.ethicalsoft.ethicalsoft_complience.infra.security.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthCommandAdapter implements AuthCommandPort {

    private final AuthAdapter authService;
    private final TokenService tokenService;
    private final RefreshTokenPort refreshTokenPort;

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
        var user = authService.loadUserByEmail(email);
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
}
