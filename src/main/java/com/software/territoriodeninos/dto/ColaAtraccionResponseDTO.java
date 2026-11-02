package com.software.territoriodeninos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "DTO de respuesta con el estado consolidado de la cola y sesiones activas en una atracción (HU-28.02)")
public record ColaAtraccionResponseDTO(
    @Schema(description = "ID de la atracción", example = "1")
    Long atraccionId,

    @Schema(description = "Nombre de la atracción", example = "Piscina de Pelotas")
    String atraccionNombre,

    @Schema(description = "Capacidad máxima permitida", example = "20")
    Integer capacidadMaxima,

    @Schema(description = "Cantidad de menores actualmente en juego", example = "14")
    Integer ocupacionActiva,

    @Schema(description = "Cupos disponibles inmediatamente", example = "6")
    Integer cuposDisponibles,

    @Schema(description = "Lista ordenada de sesiones activas (próximos a salir primero)", example = "[]")
    List<SesionJuegoResponseDTO> sesionesActivas,

    @Schema(description = "Marca de tiempo central del servidor", example = "2026-11-02T15:35:00")
    LocalDateTime timestampServidor
) {}
