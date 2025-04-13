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
    public AuthResponseDTO refresh(@Valid @RequestBody RefreshTokenRequestDTO refreshTokenRequestDTO) {
        return authenticationFacadeService.refresh(refreshTokenRequestDTO);
    }

    @PostMapping("/logout")
    public void logout(@Valid @RequestBody RefreshTokenRequestDTO refreshTokenRequestDTO) {
        refreshTokenService.deleteRefreshToken(refreshTokenRequestDTO);
    }

    @PostMapping("/register")
    public void register(@Valid @RequestBody RegisterUserDTO registerUserDTO ) {
        authService.register(registerUserDTO);
    }

    @PostMapping("/recover")
    public void requestRecovery(@Valid @RequestBody PasswordRecoveryDTO passwordRecoveryDTO) {
        recoveryService.requestRecovery(passwordRecoveryDTO);
    }

    @PostMapping("/validate-code")
    public void validateCode(@Valid @RequestBody CodeValidationDTO codeValidationDTO) {
        recoveryService.validateCode(codeValidationDTO);
    }

    @PostMapping("/reset-password")
    public void resetPassword(@Valid @RequestBody PasswordResetDTO passwordResetDTO) {
        recoveryService.resetPassword(passwordResetDTO);
    }

}
