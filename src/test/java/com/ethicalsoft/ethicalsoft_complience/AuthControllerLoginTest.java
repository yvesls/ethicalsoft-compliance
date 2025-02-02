package com.ethicalsoft.ethicalsoft_complience;

import com.ethicalsoft.ethicalsoft_complience.controller.AuthController;
import com.ethicalsoft.ethicalsoft_complience.infra.security.TokenService;
import com.ethicalsoft.ethicalsoft_complience.model.User;
import com.ethicalsoft.ethicalsoft_complience.model.dto.AuthDTO;
import com.ethicalsoft.ethicalsoft_complience.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthControllerLoginTest {

    @Mock
    private AuthService authService;
    @Mock
    private TokenService tokenService;
    @InjectMocks
    private AuthController authController;

    @Test
    void testLogin_Success() {
        Authentication authentication = mock(Authentication.class);
        when(authService.login(any(AuthDTO.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new User());
        when(tokenService.generateToken(any(User.class))).thenReturn("mocked-token");

        ResponseEntity<String> response = authController.login(new AuthDTO("user", "password"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst("Authorization")).startsWith("Bearer");
    }

    @Test
    void testLogin_InvalidCredentials() {
        AuthDTO authDTO = new AuthDTO("user", "wrongpassword");
        when(authService.login(any(AuthDTO.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThrows(BadCredentialsException.class,
                () -> authController.login(authDTO));

        verify(tokenService, never()).generateToken(any());
    }

    @Test
    void testLogin_UserNotFound() {
        AuthDTO authDTO = new AuthDTO("nonexistent", "password");
        when(authService.login(any(AuthDTO.class)))
                .thenThrow(new UsernameNotFoundException("User not found"));

        assertThrows(UsernameNotFoundException.class,
                () -> authController.login(authDTO));

        verify(tokenService, never()).generateToken(any());
    }

    @Test
    void testLogin_ServerError() {
        AuthDTO authDTO = new AuthDTO("user", "password");
        when(authService.login(any(AuthDTO.class)))
                .thenThrow(new RuntimeException("Internal server error"));

        assertThrows(RuntimeException.class,
                () -> authController.login(authDTO));

        verify(tokenService, never()).generateToken(any());
    }
}