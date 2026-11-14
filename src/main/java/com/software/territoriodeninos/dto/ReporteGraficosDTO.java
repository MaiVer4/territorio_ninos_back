package com.software.territoriodeninos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO unificado para la alimentación de gráficos estadísticos en el panel administrativo (HU-40).
 */
@Builder
public record ReporteGraficosDTO(
        @Schema(description = "Distribución horaria de ventas reales y afluencia de menores")
        List<VentaPorHoraDTO> ventasPorHora,

        @Schema(description = "Evolución diaria de recaudación para los últimos 7 días")
        List<EvolucionSemanalDTO> evolucionSemanal,

        @Schema(description = "Distribución consolidada de ingresos por método de pago")
        List<DistribucionMetodoPagoDTO> distribucionMetodosPago
) {

    @Builder
    public record VentaPorHoraDTO(
            @Schema(description = "Hora de la franja (ej: '8am' o '08:00')", example = "8am")
            String hora,

            @Schema(description = "Ventas reales recaudadas en la hora en pesos colombianos (COP)", example = "120000.00")
            BigDecimal ventasCop,

            @Schema(description = "Afluencia estimada de menores o boletas en esa hora", example = "8")
            Long afluencia
    ) {}

    @Builder
    public record EvolucionSemanalDTO(
            @Schema(description = "Fecha en formato YYYY-MM-DD", example = "2026-11-14")
            String fecha,

            @Schema(description = "Etiqueta del día de la semana abreviada (ej: 'Lun', 'Mar')", example = "Lun")
            String etiquetaDia,

            @Schema(description = "Ingresos en pesos colombianos (COP) para ese día", example = "2850000.00")
            BigDecimal ingresosCop
    ) {}

    @Builder
    public record DistribucionMetodoPagoDTO(
            @Schema(description = "Código o clave del método de pago (ej: 'EFECTIVO')", example = "EFECTIVO")
            String metodo,

            @Schema(description = "Etiqueta legible del método de pago", example = "Efectivo")
            String etiqueta,

            @Schema(description = "Monto acumulado en pesos colombianos (COP)", example = "18450000.00")
            BigDecimal montoCop,

            @Schema(description = "Porcentaje relativo sobre el recaudo total del período", example = "45.2")
            Double porcentaje
    ) {}
}
