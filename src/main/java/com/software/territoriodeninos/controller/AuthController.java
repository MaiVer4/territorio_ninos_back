package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.AuthResponseDTO;
import com.software.territoriodeninos.dto.LoginRequestDTO;
import com.software.territoriodeninos.dto.ForgotPasswordRequest;
import com.software.territoriodeninos.dto.ResetPasswordRequest;
import com.software.territoriodeninos.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthService authService;

    // =========================================================================
    // LOGIN
    // =========================================================================
    @Override
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {
        AuthResponseDTO response = authService.login(loginRequestDTO);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // RECUPERACIÓN DE CONTRASEÑA — PASO 1
    // =========================================================================
    @Override
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.generarTokenRecuperacion(request.getEmail());
        // Siempre devolvemos OK para no revelar qué correos están registrados
        return ResponseEntity.ok(java.util.Map.of("mensaje", "Si el correo existe, se enviará un enlace de recuperación."));
    }

    // =========================================================================
    // RECUPERACIÓN DE CONTRASEÑA — PASO 2
    // =========================================================================
    @Override
    @PatchMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.cambiarContrasena(request.getToken(), request.getNuevaContrasena());
        return ResponseEntity.ok(java.util.Map.of("mensaje", "Contraseña actualizada exitosamente."));
    }

    // =========================================================================
    // LOGOUT
    // =========================================================================
    @Override
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        authService.cerrarSesion(authHeader);
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        return ResponseEntity.ok(java.util.Map.of("mensaje", "Sesión cerrada exitosamente."));
    }
}
