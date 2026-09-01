package com.software.territoriodeninos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "DTO para solicitar la recuperación de contraseña")
public class ForgotPasswordRequest {
    
    @NotBlank(message = "El email es requerido")
    @Email(message = "El formato del email es inválido")
    @Schema(description = "Correo electrónico asociado a la cuenta a recuperar", example = "usuario@ejemplo.com")
    private String email;
}
