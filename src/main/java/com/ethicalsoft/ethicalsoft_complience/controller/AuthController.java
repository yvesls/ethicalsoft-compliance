package com.ethicalsoft.ethicalsoft_complience.controller;

import com.ethicalsoft.ethicalsoft_complience.controller.base.BaseController;
import com.ethicalsoft.ethicalsoft_complience.infra.security.TokenService;
import com.ethicalsoft.ethicalsoft_complience.model.User;
import com.ethicalsoft.ethicalsoft_complience.model.dto.*;
import com.ethicalsoft.ethicalsoft_complience.service.AuthService;
import com.ethicalsoft.ethicalsoft_complience.service.PasswordRecoveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RequiredArgsConstructor
@RestController
@Validated
@RequestMapping( "auth" )
public class AuthController extends BaseController {

    private final AuthService authService;

    private final TokenService tokenService;

    private final PasswordRecoveryService recoveryService;

    @PostMapping( "/login" )
    public ResponseEntity<String> login(@Valid @RequestBody AuthDTO authDTO ) {
        var auth = authService.login(authDTO);
        var token = this.tokenService.generateToken( (User) auth.getPrincipal());
        return ResponseEntity
                .ok()
                .header("Authorization", "Bearer " + token)
                .build();
    }

    @PostMapping("/register")
    public void register(@Valid @RequestBody RegisterUserDTO registerUserDTO ) {
        authService.register( registerUserDTO );
    }

    @PostMapping("/recover")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> requestRecovery(@Valid @RequestBody PasswordRecoveryDTO request) {
        recoveryService.requestRecovery(request.getEmail());
        return ResponseEntity.ok("Recovery code sent.");
    }

    @PostMapping("/validate")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> validateCode(@Valid @RequestBody CodeValidationDTO request) {
        boolean isValid = recoveryService.validateCode(request.getEmail(), request.getCode());
        return ResponseEntity.ok(isValid ? "Code is valid." : "Incorrect code.");
    }

    @PostMapping("/reset")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody PasswordResetDTO request) {
        recoveryService.resetPassword(request.getEmail(), request.getCode(), request.getNewPassword());
        return ResponseEntity.ok("Password updated successfully.");
    }

}
