package com.software.territoriodeninos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "DTO con métricas de aforo y ocupación en tiempo real de una atracción (HU-34.03)")
public record AforoActualDTO(
    @Schema(description = "ID de la atracción", example = "1")
    Long atraccionId,

    @Schema(description = "Nombre de la atracción", example = "Piscina de Pelotas")
    String atraccionNombre,

    @Schema(description = "Capacidad máxima simultánea de la atracción", example = "20")
    Integer capacidadMaxima,

    @Schema(description = "Cantidad de menores actualmente en juego (ocupación activa)", example = "18")
    Integer ocupacionActiva,

    @Schema(description = "Cupos libres disponibles inmediatamente", example = "2")
    Integer cuposLibres,

    @Schema(description = "Porcentaje actual de ocupación (0.0 a 100.0)", example = "90.0")
    Double porcentajeOcupacion,

    @Schema(description = "Indica si el aforo ha alcanzado el 100% de la capacidad", example = "false")
    boolean aforoCompleto,

    @Schema(description = "Marca de tiempo del servidor al momento del cálculo", example = "2026-11-02T15:35:00")
    LocalDateTime timestampServidor
) {}
