package com.software.territoriodeninos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "DTO de solicitud para vincular o confirmar una estación operativa por un operario")
public record VincularEstacionRequestDTO(
    @NotNull(message = "El ID de la atracción es obligatorio")
    @Schema(description = "ID de la atracción seleccionada", example = "1")
    Long atraccionId
) {}
