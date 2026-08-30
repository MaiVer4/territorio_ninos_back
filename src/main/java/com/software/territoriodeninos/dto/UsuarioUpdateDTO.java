package com.software.territoriodeninos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;

@Schema(description = "DTO para actualizar información de un usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioUpdateDTO {
    
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    @Schema(description = "Nombre completo del usuario", example = "Juan Pérez Actualizado")
    private String nombre;
    
    @Email(message = "El formato del correo no es válido")
    @Schema(description = "Correo electrónico único", example = "juan.nuevo@example.com")
    private String correo;
    
    @Schema(description = "Nuevo estado del usuario (ACTIVO, INACTIVO, SUSPENDIDO)", example = "ACTIVO")
    private String estado;
}
