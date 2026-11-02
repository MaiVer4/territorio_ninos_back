package com.software.territoriodeninos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "DTO de respuesta para calibración de reloj y sincronización de cola en terminales móviles (HU-35.01, HU-35.02)")
public record SincronizacionRelojResponseDTO(
    @Schema(description = "ID de la atracción", example = "1")
    Long atraccionId,

    @Schema(description = "Nombre de la atracción", example = "Piscina de Pelotas")
    String atraccionNombre,

    @Schema(description = "Timestamp oficial del servidor en milisegundos (Epoch Unix)", example = "1793641200000")
    long serverEpochMillis,

    @Schema(description = "Marca temporal del servidor en formato ISO", example = "2026-11-02T15:35:00")
    LocalDateTime serverTimestamp,

    @Schema(description = "Estado consolidado y calibrado de la cola de la atracción")
    ColaAtraccionResponseDTO colaActualizada
) {}
