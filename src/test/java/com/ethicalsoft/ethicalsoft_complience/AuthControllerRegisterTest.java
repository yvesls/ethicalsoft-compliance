package com.ethicalsoft.ethicalsoft_complience;

import com.ethicalsoft.ethicalsoft_complience.controller.AuthController;
import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import com.ethicalsoft.ethicalsoft_complience.model.dto.RegisterUserDTO;
import com.ethicalsoft.ethicalsoft_complience.model.enums.ErrorTypeEnum;
import com.ethicalsoft.ethicalsoft_complience.service.AuthService;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerRegisterTest {

    @Mock
    private AuthService authService;
    @InjectMocks
    private AuthController authController;

    @Test
    void register_invalidEmail() {
        RegisterUserDTO registerUserDTO = new RegisterUserDTO();
        registerUserDTO.setFirstName("John");
        registerUserDTO.setLastName("Doe");
        registerUserDTO.setEmail("invalid-email");
        registerUserDTO.setPassword("Password123!");
        registerUserDTO.setAcceptedTerms(true);

        doThrow(new ConstraintViolationException("Invalid email format", null))
                .when(authService).register(any(RegisterUserDTO.class));

        assertThrows(ConstraintViolationException.class,
                () -> authController.register(registerUserDTO));

        verify(authService, times(1)).register(any());
    }

    @Test
    void register_EmptyPassword() {
        RegisterUserDTO registerUserDTO = new RegisterUserDTO();
        registerUserDTO.setFirstName("John");
        registerUserDTO.setLastName("Doe");
        registerUserDTO.setEmail("test@example.com");
        registerUserDTO.setPassword("");
        registerUserDTO.setAcceptedTerms(true);

        doThrow(new BusinessException(ErrorTypeEnum.INFO, "Password cannot be empty"))
                .when(authService).register(any(RegisterUserDTO.class));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authController.register(registerUserDTO));

        verify(authService, times(1)).register(any());
    }

    @Test
    void register_nullEmail() {
        RegisterUserDTO registerUserDTO = new RegisterUserDTO();
        registerUserDTO.setFirstName("John");
        registerUserDTO.setLastName("Doe");
        registerUserDTO.setPassword("Password123!");
        registerUserDTO.setAcceptedTerms(true);

        doThrow(new BusinessException(ErrorTypeEnum.INFO, "Email is required"))
                .when(authService).register(any(RegisterUserDTO.class));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authController.register(registerUserDTO));

        verify(authService, times(1)).register(any());
    }

    @Test
    void register_nullPassword() {
        RegisterUserDTO registerUserDTO = new RegisterUserDTO();
        registerUserDTO.setFirstName("John");
        registerUserDTO.setLastName("Doe");
        registerUserDTO.setEmail("test@example.com");
        registerUserDTO.setAcceptedTerms(true);

        doThrow(new BusinessException(ErrorTypeEnum.INFO, "Password is required"))
                .when(authService).register(any(RegisterUserDTO.class));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authController.register(registerUserDTO));

        verify(authService, times(1)).register(any());
    }

    @Test
    void register_success() {
        RegisterUserDTO registerUserDTO = new RegisterUserDTO();
        registerUserDTO.setFirstName("John");
        registerUserDTO.setLastName("Doe");
        registerUserDTO.setEmail("test@example.com");
        registerUserDTO.setPassword("Password123!");
        registerUserDTO.setAcceptedTerms(true);

        doNothing().when(authService).register(any(RegisterUserDTO.class));

        authController.register(registerUserDTO);

        verify(authService, times(1)).register(any());
    }

    @Test
    void register_termsNotAccepted() {
        RegisterUserDTO registerUserDTO = new RegisterUserDTO();
        registerUserDTO.setFirstName("John");
        registerUserDTO.setLastName("Doe");
        registerUserDTO.setEmail("test@example.com");
        registerUserDTO.setPassword("Password123!");
        registerUserDTO.setAcceptedTerms(false);

        doThrow(new BusinessException(ErrorTypeEnum.INFO, "The terms were not accepted"))
                .when(authService).register(any(RegisterUserDTO.class));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authController.register(registerUserDTO));

        verify(authService, times(1)).register(any());
    }

}
