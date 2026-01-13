package com.ethicalsoft.ethicalsoft_complience.application.port;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.User;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.auth.RefreshTokenDTO;

public interface RefreshTokenPort {
    String createRefreshToken(User user);

    String validateRefreshToken(RefreshTokenDTO refreshTokenDTO);

    void deleteRefreshToken(RefreshTokenDTO refreshTokenDTO);
}

