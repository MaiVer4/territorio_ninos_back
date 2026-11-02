package com.software.territoriodeninos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "DTO de solicitud para iniciar formalmente la sesión de juego de un menor tras lectura QR (HU-27.03)")
public record InicioSesionRequestDTO(
    @NotNull(message = "El UUID del ticket es obligatorio")
    @Schema(description = "Código UUID del ticket", example = "93957dbe-823c-4b57-b5e5-629857541d06")
    UUID uuidTicket,

    @NotNull(message = "El ID de la atracción es obligatorio")
    @Schema(description = "ID de la atracción donde ingresa el menor", example = "1")
    Long atraccionId,

    @Schema(description = "Marca temporal del cliente móvil al pulsar inicio", example = "2026-11-02T15:30:00")
    LocalDateTime timestamp
) {}
