package com.software.territoriodeninos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "DTO de respuesta con el token JWT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseDTO {
    
    @Schema(description = "Token JWT generado para el usuario autenticado")
    private String token;
    
    @Schema(description = "Tipo de token (Bearer)")
    private String tipoToken;
    
    @Schema(description = "ID del usuario autenticado")
    private Long id;
    
    @Schema(description = "Nombre del usuario autenticado")
    private String nombre;
    
    @Schema(description = "Correo del usuario autenticado")
    private String correo;
    
    @Schema(description = "Rol del usuario autenticado")
    private String rol;
}
