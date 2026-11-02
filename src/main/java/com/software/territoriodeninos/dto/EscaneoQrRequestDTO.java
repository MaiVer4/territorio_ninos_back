package com.software.territoriodeninos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "DTO de solicitud para lectura y validación de código QR en puesto de acceso (HU-25.01)")
public record EscaneoQrRequestDTO(
    @NotNull(message = "El UUID del ticket es obligatorio")
    @Schema(description = "Código UUID escaneado de la boleta", example = "93957dbe-823c-4b57-b5e5-629857541d06")
    UUID uuid,

    @NotNull(message = "El ID de la atracción es obligatorio")
    @Schema(description = "ID de la atracción donde se ubica el operario escaneando", example = "1")
    Long atraccionId,

    @Schema(description = "Marca temporal del escaneo en la terminal móvil", example = "2026-11-02T15:30:00")
    LocalDateTime timestamp
) {}
