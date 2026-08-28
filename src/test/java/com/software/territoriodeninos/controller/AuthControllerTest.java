package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.ForgotPasswordRequest;
import com.software.territoriodeninos.dto.ResetPasswordRequest;
import com.software.territoriodeninos.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @Test
    void testForgotPassword() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("test@test.com");
        
        doNothing().when(authService).generarTokenRecuperacion("test@test.com");
        
        ResponseEntity<?> response = authController.forgotPassword(request);
        
        assertEquals(200, response.getStatusCode().value());
        assertEquals("Si el correo existe, se enviará un enlace de recuperación.", response.getBody());
        verify(authService, times(1)).generarTokenRecuperacion("test@test.com");
    }

    @Test
    void testResetPassword() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("my-token");
        request.setNuevaContrasena("NuevaPass123");
        
        doNothing().when(authService).cambiarContrasena("my-token", "NuevaPass123");
        
        ResponseEntity<?> response = authController.resetPassword(request);
        
        assertEquals(200, response.getStatusCode().value());
        assertEquals("Contraseña actualizada exitosamente.", response.getBody());
        verify(authService, times(1)).cambiarContrasena("my-token", "NuevaPass123");
    }
}
