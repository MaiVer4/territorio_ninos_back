package com.software.territoriodeninos.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "DTO para asignar un operario a una atracción física")
public record AsignacionRequestDTO(
    @JsonAlias({"operarioId", "usuarioId"})
    @NotNull(message = "El ID del operario es obligatorio")
    @Schema(description = "ID del usuario con rol OPERARIO", example = "5")
    Long usuarioId,

    @NotNull(message = "El ID de la atracción es obligatorio")
    @Schema(description = "ID de la atracción", example = "1")
    Long atraccionId,

    @Schema(description = "Observaciones o detalles del turno", example = "Turno mañana")
    String observaciones,

    @Schema(description = "Indica si se debe forzar la reasignación si el operario ya cuenta con turno activo", example = "false")
    Boolean forzarReasignacion
) {
    public AsignacionRequestDTO(Long usuarioId, Long atraccionId, String observaciones) {
        this(usuarioId, atraccionId, observaciones, null);
    }
}

