package com.software.territoriodeninos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "DTO de solicitud para congelar simultáneamente todas las sesiones de una atracción (HU-29.05)")
public record PausaGeneralRequestDTO(
    @NotBlank(message = "El motivo de la pausa general es obligatorio")
    @Size(max = 500, message = "El motivo no puede exceder 500 caracteres")
    @Schema(description = "Motivo de la interrupción general (ej. falla técnica, lluvia, limpieza)", example = "Falla técnica en motor de la atracción")
    String motivo
) {}
