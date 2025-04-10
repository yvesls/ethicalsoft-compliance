package com.ethicalsoft.ethicalsoft_complience.controller;

import com.ethicalsoft.ethicalsoft_complience.controller.base.BaseController;
import com.ethicalsoft.ethicalsoft_complience.model.dto.*;
import com.ethicalsoft.ethicalsoft_complience.model.dto.request.RefreshTokenRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.model.dto.response.AuthResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.service.AuthService;
import com.ethicalsoft.ethicalsoft_complience.service.PasswordRecoveryService;
import com.ethicalsoft.ethicalsoft_complience.service.RefreshTokenService;
import com.ethicalsoft.ethicalsoft_complience.service.facade.AuthenticationFacadeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@Validated
@RequestMapping( "auth" )
public class AuthController extends BaseController {

    private final AuthService authService;

    private final RefreshTokenService refreshTokenService;

    private final PasswordRecoveryService recoveryService;

    private final AuthenticationFacadeService authenticationFacadeService;

    @PostMapping("/token")
    public AuthResponseDTO token(@Valid @RequestBody AuthDTO authDTO) {
        return authenticationFacadeService.token(authDTO);
    }

    @PostMapping("/refresh")
    public AuthResponseDTO refresh(@Valid @RequestBody RefreshTokenRequestDTO request) {
        return authenticationFacadeService.refresh(request);
    }

    @PostMapping("/logout")
    public void logout(@Valid @RequestBody RefreshTokenRequestDTO request) {
        refreshTokenService.deleteRefreshToken(request.getRefreshToken());
    }

    @PostMapping("/register")
    public void register(@Valid @RequestBody RegisterUserDTO registerUserDTO ) {
        authService.register( registerUserDTO );
    }

    @PostMapping("/recover")
    public void requestRecovery(@Valid @RequestBody PasswordRecoveryDTO request) {
        recoveryService.requestRecovery(request.getEmail());
    }

    @PostMapping("/validate-code")
    public void validateCode(@Valid @RequestBody CodeValidationDTO request) {
        recoveryService.validateCode(request.getEmail(), request.getCode());
    }

    @PostMapping("/reset-password")
    public void resetPassword(@Valid @RequestBody PasswordResetDTO request) {
        recoveryService.resetPassword(request.getEmail(), request.getNewPassword());
    }

}
