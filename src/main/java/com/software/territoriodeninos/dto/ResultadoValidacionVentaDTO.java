package com.software.territoriodeninos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.Map;

@Schema(description = "Resultado de la validación de aforo y consistencia de tarifas")
public record ResultadoValidacionVentaDTO(
    @Schema(description = "Indica si la orden completa es válida", example = "true")
    boolean valido,

    @Schema(description = "Total general calculado con precios oficiales de base de datos", example = "45000.00")
    BigDecimal totalOficial,

    @Schema(description = "Cantidad total de tickets validados", example = "3")
    int totalTickets,

    @Schema(description = "Mapa de cupos restantes proyectados por ID de atracción tras la venta")
    Map<Long, Integer> aforoRestantePorAtraccion
) {}
