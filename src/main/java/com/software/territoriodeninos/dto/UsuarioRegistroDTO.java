package com.software.territoriodeninos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Schema(description = "DTO para registrar un nuevo usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioRegistroDTO {
    
    @NotBlank(message = "El nombre no puede estar vacío")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    @Schema(description = "Nombre completo del usuario", example = "Juan Pérez")
    private String nombre;
    
    @NotBlank(message = "El correo no puede estar vacío")
    @Email(message = "El formato del correo no es válido")
    @Schema(description = "Correo electrónico único", example = "juan@example.com")
    private String correo;
    
    @NotBlank(message = "La contraseña no puede estar vacía")
    @com.fasterxml.jackson.annotation.JsonAlias({"contrasena", "contraseña", "clave"})
    @jakarta.validation.constraints.Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$", 
        message = "La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula y un número"
    )
    @Schema(description = "Contraseña del usuario (al menos 8 caracteres, 1 mayúscula, 1 minúscula, 1 número)", example = "Password123")
    private String password;
    
    @NotBlank(message = "El rol no puede estar vacío")
    @com.fasterxml.jackson.annotation.JsonAlias({"rol", "role"})
    @Schema(description = "Nombre del rol (ADMINISTRADOR, TAQUILLERO, OPERARIO)", example = "TAQUILLERO")
    private String nombreRol;
}
