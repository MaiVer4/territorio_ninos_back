package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.AuthResponseDTO;
import com.software.territoriodeninos.dto.ForgotPasswordRequest;
import com.software.territoriodeninos.dto.LoginRequestDTO;
import com.software.territoriodeninos.dto.ResetPasswordRequest;
import com.software.territoriodeninos.exception.CredencialesInvalidasException;
import com.software.territoriodeninos.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @Test
    void testLogin_Success() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setCorreo("test@test.com");
        request.setPassword("Password123");
        
        AuthResponseDTO mockResponse = new AuthResponseDTO();
        mockResponse.setToken("mockToken");
        
        when(authService.login(any(LoginRequestDTO.class))).thenReturn(mockResponse);
        
        ResponseEntity<AuthResponseDTO> response = authController.login(request);
        
        assertEquals(200, response.getStatusCode().value());
        assertEquals("mockToken", response.getBody().getToken());
    }

    @Test
    void testLogin_CredencialesInvalidas() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setCorreo("test@test.com");
        request.setPassword("WrongPass");
        
        when(authService.login(any(LoginRequestDTO.class))).thenThrow(new CredencialesInvalidasException("Correo o contraseña incorrectos"));
        
        assertThrows(CredencialesInvalidasException.class, () -> authController.login(request));
    }

    @Test
    void testLogin_UsuarioInactivo() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setCorreo("inactivo@test.com");
        request.setPassword("Password123");
        
        when(authService.login(any(LoginRequestDTO.class))).thenThrow(new CredencialesInvalidasException("Correo o contraseña incorrectos"));
        
        assertThrows(CredencialesInvalidasException.class, () -> authController.login(request));
    }

    @Test
    void testForgotPassword() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("test@test.com");
        
        doNothing().when(authService).generarTokenRecuperacion("test@test.com");
        
        ResponseEntity<?> response = authController.forgotPassword(request);
        
        assertEquals(200, response.getStatusCode().value());
        assertEquals(java.util.Map.of("mensaje", "Si el correo existe, se enviará un enlace de recuperación."), response.getBody());
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
        assertEquals(java.util.Map.of("mensaje", "Contraseña actualizada exitosamente."), response.getBody());
        verify(authService, times(1)).cambiarContrasena("my-token", "NuevaPass123");
    }

    @Test
    void testLogout_Success() {
        // Ejecutamos logout que simplemente limpia el contexto (el cual por defecto estará vacío aquí) y retorna 200
        ResponseEntity<?> response = authController.logout();
        
        assertEquals(200, response.getStatusCode().value());
        assertEquals(java.util.Map.of("mensaje", "Sesión cerrada exitosamente."), response.getBody());
    }
}
