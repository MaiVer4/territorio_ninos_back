package com.software.territoriodeninos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "DTO de solicitud para pausar una sesión individual de juego por contingencia (HU-29.03)")
public record PausaRequestDTO(
    @NotBlank(message = "El motivo de la pausa es obligatorio")
    @Size(max = 500, message = "El motivo no puede exceder 500 caracteres")
    @Schema(description = "Motivo de la pausa (ej. llanto, calzado, baño)", example = "Menor requiere ir al servicio sanitario")
    String motivo
) {}
