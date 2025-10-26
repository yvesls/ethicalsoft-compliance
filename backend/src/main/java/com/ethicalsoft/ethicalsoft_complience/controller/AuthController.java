package com.ethicalsoft.ethicalsoft_complience.controller;

import com.ethicalsoft.ethicalsoft_complience.controller.base.BaseController;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.auth.*;
import com.ethicalsoft.ethicalsoft_complience.service.AuthService;
import com.ethicalsoft.ethicalsoft_complience.service.PasswordRecoveryService;
import com.ethicalsoft.ethicalsoft_complience.service.RefreshTokenService;
import com.ethicalsoft.ethicalsoft_complience.service.facade.AuthenticationFacadeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@Validated
@RequestMapping( "auth" )
public class AuthController extends BaseController {

	private final AuthService authService;

	private final RefreshTokenService refreshTokenService;

	private final PasswordRecoveryService recoveryService;

	private final AuthenticationFacadeService authenticationFacadeService;

	@PostMapping( "/token" )
	public AuthDTO token( @Valid @RequestBody LoginDTO loginDTO ) {
		return authenticationFacadeService.token( loginDTO );
	}

	@PostMapping( "/refresh" )
	public AuthDTO refresh( @Valid @RequestBody RefreshTokenDTO refreshTokenDTO ) {
		return authenticationFacadeService.refresh( refreshTokenDTO );
	}

	@PostMapping( "/logout" )
	public void logout( @Valid @RequestBody RefreshTokenDTO refreshTokenDTO ) {
		refreshTokenService.deleteRefreshToken( refreshTokenDTO );
	}

	@PostMapping( "/register" )
	public void register( @Valid @RequestBody RegisterUserDTO registerUserDTO ) {
		authService.register( registerUserDTO );
	}

	@PostMapping( "/recover-account" )
	public void requestRecovery( @Valid @RequestBody PasswordRecoveryDTO passwordRecoveryDTO ) {
		recoveryService.requestRecovery( passwordRecoveryDTO );
	}

	@PostMapping( "/validate-code" )
	public void validateCode( @Valid @RequestBody CodeValidationDTO codeValidationDTO ) {
		recoveryService.validateCode( codeValidationDTO );
	}

	@PostMapping( "/reset-password" )
	public void resetPassword( @Valid @RequestBody PasswordResetDTO passwordResetDTO ) {
		recoveryService.resetPassword( passwordResetDTO );
	}

}
