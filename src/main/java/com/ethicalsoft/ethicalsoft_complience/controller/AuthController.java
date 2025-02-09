package com.ethicalsoft.ethicalsoft_complience.controller;

import com.ethicalsoft.ethicalsoft_complience.controller.base.BaseController;
import com.ethicalsoft.ethicalsoft_complience.infra.security.TokenService;
import com.ethicalsoft.ethicalsoft_complience.model.User;
import com.ethicalsoft.ethicalsoft_complience.model.dto.*;
import com.ethicalsoft.ethicalsoft_complience.service.AuthService;
import com.ethicalsoft.ethicalsoft_complience.service.PasswordRecoveryService;
import jakarta.servlet.http.HttpServletResponse;
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

    private final TokenService tokenService;

    private final PasswordRecoveryService recoveryService;

    @PostMapping("/login")
    public void login(@Valid @RequestBody AuthDTO authDTO, HttpServletResponse response) {
        var auth = authService.login(authDTO);
        var token = this.tokenService.generateToken((User) auth.getPrincipal());
        response.addHeader("Authorization", "Bearer " + token);
    }

    @PostMapping("/register")
    public void register(@Valid @RequestBody RegisterUserDTO registerUserDTO ) {
        authService.register( registerUserDTO );
    }

    @PostMapping("/recover")
    @PreAuthorize("hasRole('USER')")
    public void requestRecovery(@Valid @RequestBody PasswordRecoveryDTO request) {
        recoveryService.requestRecovery(request.getEmail());
    }

    @PostMapping("/validate")
    @PreAuthorize("hasRole('USER')")
    public void validateCode(@Valid @RequestBody CodeValidationDTO request) {
        recoveryService.validateCode(request.getEmail(), request.getCode());
    }

    @PostMapping("/reset")
    @PreAuthorize("hasRole('USER')")
    public void resetPassword(@Valid @RequestBody PasswordResetDTO request) {
        recoveryService.resetPassword(request.getEmail(), request.getCode(), request.getNewPassword());
    }

}
