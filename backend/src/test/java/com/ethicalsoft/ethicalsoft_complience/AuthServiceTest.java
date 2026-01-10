package com.ethicalsoft.ethicalsoft_complience;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.auth.AuthAdapter;
import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.User;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.auth.LoginDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.auth.RegisterUserDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthAdapter authService;

    private RegisterUserDTO validRegisterDTO;
    private LoginDTO validLoginDTO;

    @BeforeEach
    void setUp() {
        validRegisterDTO = new RegisterUserDTO();
        validRegisterDTO.setEmail("test@example.com");
        validRegisterDTO.setPassword("password123");
        validRegisterDTO.setAcceptedTerms(true);

        validLoginDTO = new LoginDTO();
        validLoginDTO.setUsername("test@example.com");
        validLoginDTO.setPassword("password123");
    }

    @Test
    void login_Success() {
        Authentication expectedAuth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(expectedAuth);

        Authentication result = authService.token(validLoginDTO);

        assertEquals(expectedAuth, result);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_InvalidCredentials() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.token(validLoginDTO));
    }

    @Test
    void register_Success() {
        when(userRepository.findByEmail(validRegisterDTO.getEmail()))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any(User.class)))
                .thenReturn(new User());

        assertDoesNotThrow(() -> authService.register(validRegisterDTO));

        verify(userRepository).findByEmail(validRegisterDTO.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_EmailAlreadyExists() {
        when(userRepository.findByEmail(validRegisterDTO.getEmail()))
                .thenReturn(Optional.of(new User()));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.register(validRegisterDTO));

        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository).findByEmail(validRegisterDTO.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_TermsNotAccepted() {
        validRegisterDTO.setAcceptedTerms(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.register(validRegisterDTO));

        assertEquals("The terms were not accepted", exception.getMessage());
        verify(userRepository, never()).findByEmail(any());
        verify(userRepository, never()).save(any());
    }
}