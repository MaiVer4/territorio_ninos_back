package com.software.territoriodeninos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Solicitud para autorizar el ingreso por sobreaforo en una atracción")
public record AutorizarSobreaforoRequestDTO(
    @NotNull(message = "El ID de la atracción es obligatorio")
    @Schema(description = "ID de la atracción donde se autoriza el sobreaforo", example = "1")
    Long atraccionId,

    @NotBlank(message = "La clave o PIN de supervisor es obligatoria")
    @Size(max = 128, message = "La clave de supervisor no puede superar 128 caracteres")
    @Schema(description = "PIN operativo configurado o contraseña de administrador/coordinador")
    String claveSupervisor,

    @Size(max = 500, message = "El motivo no puede superar los 500 caracteres")
    @Schema(description = "Motivo operativo de la autorización", example = "Grupo familiar con un menor adicional")
    String motivo
) {}
