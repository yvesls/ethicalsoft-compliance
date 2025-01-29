package com.ethicalsoft.ethicalsoft_complience.controller;

import com.ethicalsoft.ethicalsoft_complience.controller.base.BaseController;
import com.ethicalsoft.ethicalsoft_complience.infra.security.TokenService;
import com.ethicalsoft.ethicalsoft_complience.model.User;
import com.ethicalsoft.ethicalsoft_complience.model.dto.AuthDTO;
import com.ethicalsoft.ethicalsoft_complience.model.dto.RegisterUserDTO;
import com.ethicalsoft.ethicalsoft_complience.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping( "auth" )
public class AuthController extends BaseController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    @PostMapping( "/login" )
    public ResponseEntity getUser(@RequestBody @Valid AuthDTO authDTO ) {
        var auth = authService.login(authDTO, authenticationManager);
        var token = this.tokenService.generateToken( (User) auth.getPrincipal());
        return ResponseEntity
                .ok()
                .header("Authorization", "Bearer " + token)
                .build();
    }

    @PostMapping("/register")
    public ResponseEntity getUser(@RequestBody @Valid RegisterUserDTO registerUserDTO ) {
        authService.register( registerUserDTO );
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
