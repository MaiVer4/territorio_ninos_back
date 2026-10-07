package com.software.territoriodeninos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "DTO para asignar un operario a una atracción física")
public record AsignacionRequestDTO(
    @NotNull(message = "El ID del operario es obligatorio")
    @Schema(description = "ID del usuario con rol OPERARIO", example = "5")
    Long usuarioId,

    @NotNull(message = "El ID de la atracción es obligatorio")
    @Schema(description = "ID de la atracción", example = "1")
    Long atraccionId,

    @Schema(description = "Observaciones o detalles del turno", example = "Turno mañana")
    String observaciones
) {}
