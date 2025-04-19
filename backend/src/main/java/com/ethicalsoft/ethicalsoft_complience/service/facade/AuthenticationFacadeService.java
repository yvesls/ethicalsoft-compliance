package com.ethicalsoft.ethicalsoft_complience.service.facade;

import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import com.ethicalsoft.ethicalsoft_complience.infra.security.TokenService;
import com.ethicalsoft.ethicalsoft_complience.model.User;
import com.ethicalsoft.ethicalsoft_complience.model.dto.auth.AuthDTO;
import com.ethicalsoft.ethicalsoft_complience.model.dto.auth.LoginDTO;
import com.ethicalsoft.ethicalsoft_complience.model.dto.auth.RefreshTokenDTO;
import com.ethicalsoft.ethicalsoft_complience.service.AuthService;
import com.ethicalsoft.ethicalsoft_complience.service.RefreshTokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationFacadeService {

	private final AuthService authService;
	private final TokenService tokenService;
	private final RefreshTokenService refreshTokenService;

	public AuthenticationFacadeService( AuthService authService, TokenService tokenService, RefreshTokenService refreshTokenService ) {
		this.authService = authService;
		this.tokenService = tokenService;
		this.refreshTokenService = refreshTokenService;
	}

	@Transactional( rollbackFor = Exception.class )
	public AuthDTO token( LoginDTO loginDTO ) {
		var auth = authService.token( loginDTO );
		var user = ( User ) auth.getPrincipal();
		var accessToken = tokenService.generateToken( user );
		var refreshToken = refreshTokenService.createRefreshToken( user );
		return new AuthDTO( accessToken, refreshToken );
	}

	@Transactional( rollbackFor = Exception.class )
	public AuthDTO refresh( RefreshTokenDTO request ) {
		var userEmail = refreshTokenService.validateRefreshToken( request.getRefreshToken() );
		if ( userEmail == null ) {
			throw new BusinessException( "Invalid refresh token" );
		}
		var user = refreshTokenService.getUserFromRefreshToken( request.getRefreshToken() );
		var accessToken = tokenService.generateToken( user );
		var newRefreshToken = refreshTokenService.createRefreshToken( user );
		refreshTokenService.deleteRefreshToken( request );
		return new AuthDTO( accessToken, newRefreshToken );
	}
}
