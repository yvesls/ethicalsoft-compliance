package com.ethicalsoft.ethicalsoft_complience.controller;

import com.ethicalsoft.ethicalsoft_complience.application.usecase.auth.*;
import com.ethicalsoft.ethicalsoft_complience.controller.base.BaseController;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.auth.*;
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

    private final TokenUseCase tokenUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final RegisterUseCase registerUseCase;
    private final RequestRecoveryUseCase requestRecoveryUseCase;
    private final ValidateCodeUseCase validateCodeUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;

    @PostMapping( "/token" )
    public AuthDTO token( @Valid @RequestBody LoginDTO loginDTO ) {
        return tokenUseCase.execute( loginDTO );
    }

    @PostMapping( "/refresh" )
    public AuthDTO refresh( @Valid @RequestBody RefreshTokenDTO refreshTokenDTO ) {
        return refreshTokenUseCase.execute( refreshTokenDTO );
    }

    @PostMapping( "/logout" )
    public void logout( @Valid @RequestBody RefreshTokenDTO refreshTokenDTO ) {
        logoutUseCase.execute( refreshTokenDTO );
    }

    @PostMapping( "/register" )
    public void register( @Valid @RequestBody RegisterUserDTO registerUserDTO ) {
        registerUseCase.execute( registerUserDTO );
    }

    @PostMapping( "/recover-account" )
    public void requestRecovery( @Valid @RequestBody PasswordRecoveryDTO passwordRecoveryDTO ) {
        requestRecoveryUseCase.execute( passwordRecoveryDTO );
    }

    @PostMapping( "/validate-code" )
    public void validateCode( @Valid @RequestBody CodeValidationDTO codeValidationDTO ) {
        validateCodeUseCase.execute( codeValidationDTO );
    }

    @PostMapping( "/reset-password" )
    public void resetPassword( @Valid @RequestBody PasswordResetDTO passwordResetDTO ) {
        resetPasswordUseCase.execute( passwordResetDTO );
    }
}
