package com.software.territoriodeninos.controller;

import com.software.territoriodeninos.dto.AuthResponseDTO;
import com.software.territoriodeninos.dto.LoginRequestDTO;
import com.software.territoriodeninos.dto.ForgotPasswordRequest;
import com.software.territoriodeninos.dto.ResetPasswordRequest;
import com.software.territoriodeninos.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Endpoints para autenticación, login y recuperación de contraseña")
public class AuthController {
    
    private final AuthService authService;
    
    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    
    @PostMapping("/login")
    @Operation(summary = "Autenticar usuario", description = "Login con credenciales para obtener token JWT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Autenticación exitosa",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    })
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {
        AuthResponseDTO response = authService.login(loginRequestDTO);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Solicitar recuperación de contraseña", description = "Genera un token y envía un correo (CU-03)")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.generarTokenRecuperacion(request.getEmail());
        // Siempre devolvemos OK para no revelar qué correos están registrados
        return ResponseEntity.ok("Si el correo existe, se enviará un enlace de recuperación.");
    }

    @PatchMapping("/reset-password")
    @Operation(summary = "Cambiar contraseña", description = "Usa el token UUID para cambiar la clave (CU-04)")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.cambiarContrasena(request.getToken(), request.getNuevaContrasena());
        return ResponseEntity.ok("Contraseña actualizada exitosamente.");
    }
}
