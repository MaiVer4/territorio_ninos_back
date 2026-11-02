package com.software.territoriodeninos.dto;

import com.software.territoriodeninos.util.TipoAtraccion;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record RotacionAtraccionDTO(
        @Schema(description = "ID de la atracción", example = "1")
        Long atraccionId,

        @Schema(description = "Nombre comercial de la atracción", example = "Inflable Selva Mágica")
        String nombreAtraccion,

        @Schema(description = "Tipo de atracción: TEMPORIZADA o EVENTOS_CAIDAS")
        TipoAtraccion tipoAtraccion,

        @Schema(description = "Capacidad máxima instalada de menores en simultáneo", example = "15")
        Integer capacidadMaxima,

        @Schema(description = "Cantidad total de usos o sesiones realizadas en el periodo", example = "42")
        Long cantidadTotalUsos,

        @Schema(description = "Ingresos brutos acumulados generados en el periodo", example = "250000.00")
        BigDecimal ingresosBrutos,

        @Schema(description = "Tiempo promedio de permanencia en minutos", example = "24.5")
        Double tiempoPromedioMinutos,

        @Schema(description = "Porcentaje de ocupación relativa en el periodo (%)", example = "68.5")
        Double porcentajeOcupacion,

        @Schema(description = "Franja horaria de mayor afluencia detectada", example = "16:00 - 17:00")
        String horaMayorAfluencia
) {}
