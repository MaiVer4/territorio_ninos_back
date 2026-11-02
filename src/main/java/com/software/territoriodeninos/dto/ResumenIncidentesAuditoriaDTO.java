package com.software.territoriodeninos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
public record ResumenIncidentesAuditoriaDTO(
        @Schema(description = "Total de incidentes registrados en el periodo")
        Integer totalIncidentes,

        @Schema(description = "Tiempo total acumulado de inactividad en segundos")
        Long tiempoTotalInactividadSegundos,

        @Schema(description = "Tiempo total acumulado de inactividad en minutos")
        Double tiempoTotalInactividadMinutos,

        @Schema(description = "Tiempo de inactividad por causas operativas (segundos)")
        Long inactividadOperativaSegundos,

        @Schema(description = "Tiempo de inactividad por fallas técnicas o mantenimiento (segundos)")
        Long inactividadTecnicaSegundos,

        @Schema(description = "Listado detallado de incidentes")
        List<IncidenteDetalleDTO> incidentes
) {}
