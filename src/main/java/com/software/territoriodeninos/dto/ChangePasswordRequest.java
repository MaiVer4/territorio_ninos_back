package com.software.territoriodeninos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "DTO para cambiar la contraseña de un usuario autenticado")
public class ChangePasswordRequest {
    @NotBlank(message = "La contraseña actual es requerida")
    @Schema(description = "Contraseña actual del usuario", example = "OldPassword123")
    private String passwordActual;

    @NotBlank(message = "La nueva contraseña es requerida")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$", 
        message = "La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula y un número"
    )
    @Schema(description = "Nueva contraseña del usuario", example = "NewPassword123")
    private String nuevaContrasena;
}
