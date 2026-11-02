package com.software.territoriodeninos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
public record DatasetGraficoDTO(
        @Schema(description = "Nombre o identificador de la serie de datos", example = "Afluencia por Horas Pico")
        String nombreSerie,

        @Schema(description = "Puntos de datos del gráfico")
        List<PuntoGraficoDTO> puntos
) {

    @Builder
    public record PuntoGraficoDTO(
            @Schema(description = "Etiqueta del eje X o categoría", example = "15:00 - 16:00")
            String etiqueta,

            @Schema(description = "Valor numérico absoluto", example = "42.0")
            Double valor,

            @Schema(description = "Porcentaje relativo respecto al total (%)", example = "25.5")
            Double porcentajeRelativo
    ) {}
}
