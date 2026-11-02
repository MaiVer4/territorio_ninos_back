package com.software.territoriodeninos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

@Schema(description = "DTO de solicitud para descontar un intento o registrar una ronda en atracción por evento (Toro Mecánico) (HU-33.02)")
public record RegistroIntentoEventoDTO(
    @Min(value = 1, message = "El número de intento consumido debe ser mayor o igual a 1")
    @Schema(description = "Número correlativo del intento consumido (ej. 1, 2, 3)", example = "1")
    Integer intentoConsumido,

    @Min(value = 0, message = "La duración de la ronda no puede ser negativa")
    @Schema(description = "Duración en segundos que permaneció en la ronda antes de la caída", example = "28")
    Integer segundosRonda,

    @Schema(description = "Observaciones o notas del operador", example = "Caída hacia el lateral derecho")
    String observaciones
) {}
