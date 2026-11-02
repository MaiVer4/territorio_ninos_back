package com.software.territoriodeninos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record GraficoTendenciaDTO(
        @Schema(description = "Título del reporte estadístico", example = "Horas Pico de Afluencia")
        String titulo,

        @Schema(description = "Periodo temporal analizado", example = "2026-11-01 al 2026-11-02")
        String periodo,

        @Schema(description = "Total acumulado de la métrica", example = "150.0")
        Double totalAcumulado,

        @Schema(description = "Conjunto de datos estructurado")
        DatasetGraficoDTO dataset
) {}
