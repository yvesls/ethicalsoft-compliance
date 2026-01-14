package com.ethicalsoft.ethicalsoft_complience.application.port.auth;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.auth.AuthDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.auth.LoginDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.auth.RefreshTokenDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.auth.RegisterUserDTO;

public interface AuthCommandPort {
    AuthDTO token(LoginDTO loginDTO);
    AuthDTO refresh(RefreshTokenDTO refreshTokenDTO);
    void logout(RefreshTokenDTO refreshTokenDTO);
    void register(RegisterUserDTO registerUserDTO);
}

