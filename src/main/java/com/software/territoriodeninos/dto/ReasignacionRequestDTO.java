package com.software.territoriodeninos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "DTO para reasignar un operario a otra atracción en tiempo real")
public record ReasignacionRequestDTO(
    @NotNull(message = "El ID del operario es obligatorio")
    @Schema(description = "ID del operario a reasignar", example = "5")
    Long usuarioId,

    @NotNull(message = "El ID de la nueva atracción es obligatorio")
    @Schema(description = "ID de la nueva atracción", example = "2")
    Long nuevaAtraccionId,

    @Schema(description = "Motivo de la reasignación", example = "Rotación de personal por alta afluencia")
    String motivo
) {}
