package com.ethicalsoft.ethicalsoft_complience.application.port.auth;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.auth.*;

public interface AuthCommandPort {
    AuthDTO token(LoginDTO loginDTO);
    AuthDTO refresh(RefreshTokenDTO refreshTokenDTO);
    void logout(RefreshTokenDTO refreshTokenDTO);
    void register(RegisterUserDTO registerUserDTO);
    AuthDTO googleAuth(GoogleAuthDTO googleAuthDTO);
    void acceptTerms(AcceptTermsDTO acceptTermsDTO);
    ExtendSessionResponseDTO extendSession(ExtendSessionDTO extendSessionDTO);
}

