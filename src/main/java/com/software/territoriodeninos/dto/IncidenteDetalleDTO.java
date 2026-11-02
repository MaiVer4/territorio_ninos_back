package com.software.territoriodeninos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record IncidenteDetalleDTO(
        @Schema(description = "ID del registro de incidente o pausa")
        Long incidenteId,

        @Schema(description = "ID de la atracción afectada")
        Long atraccionId,

        @Schema(description = "Nombre de la atracción")
        String atraccionNombre,

        @Schema(description = "Tipo de parada: OPERATIVA, TECNICA, CLIMATICA, EMERGENCIA")
        String tipoPausa,

        @Schema(description = "Motivo o justificación de la parada")
        String motivo,

        @Schema(description = "Indica si fue una pausa generalizada de la estación")
        Boolean esPausaGeneral,

        @Schema(description = "ID del operario que accionó la pausa")
        Long operarioId,

        @Schema(description = "Nombre del operario")
        String operarioNombre,

        @Schema(description = "Fecha y hora de inicio de la interrupción")
        LocalDateTime fechaInicioPausa,

        @Schema(description = "Fecha y hora de reanudación")
        LocalDateTime fechaFinPausa,

        @Schema(description = "Duración total de la inactividad en segundos")
        Integer duracionSegundos,

        @Schema(description = "Duración en minutos (con 2 decimales)")
        Double duracionMinutos
) {}
