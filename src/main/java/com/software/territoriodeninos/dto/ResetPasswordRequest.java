package com.software.territoriodeninos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "DTO para confirmar el cambio de contraseña usando un token de recuperación")
public class ResetPasswordRequest {
    
    @NotBlank(message = "El token es requerido")
    @Schema(description = "Token UUID enviado por correo electrónico", example = "550e8400-e29b-41d4-a716-446655440000")
    private String token;

    @NotBlank(message = "La nueva contraseña es requerida")
    @jakarta.validation.constraints.Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$", 
        message = "La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula y un número"
    )
    @Schema(description = "Nueva contraseña del usuario", example = "NuevaPass123")
    private String nuevaContrasena;
}
