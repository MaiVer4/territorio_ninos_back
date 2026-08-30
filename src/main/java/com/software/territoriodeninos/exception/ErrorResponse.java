package com.software.territoriodeninos.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Schema(description = "DTO de respuesta de error")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {
    
    @Schema(description = "Código HTTP del error")
    private int codigo;
    
    @Schema(description = "Mensaje de error")
    private String mensaje;
    
    @Schema(description = "Detalles adicionales del error")
    private Object detalles;
    
    @Builder.Default
    @Schema(description = "Timestamp del error")
    private LocalDateTime timestamp = LocalDateTime.now();
    
    public ErrorResponse(int codigo, String mensaje) {
        this.codigo = codigo;
        this.mensaje = mensaje;
        this.timestamp = LocalDateTime.now();
    }
    
    public ErrorResponse(int codigo, String mensaje, Object detalles) {
        this.codigo = codigo;
        this.mensaje = mensaje;
        this.detalles = detalles;
        this.timestamp = LocalDateTime.now();
    }
}
