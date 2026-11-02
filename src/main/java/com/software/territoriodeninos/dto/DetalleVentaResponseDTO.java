package com.software.territoriodeninos.dto;

import com.software.territoriodeninos.util.TipoTarifa;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Detalle individual de una venta procesada")
public record DetalleVentaResponseDTO(
    @Schema(description = "ID del detalle de venta", example = "1")
    Long id,

    @Schema(description = "ID de la atracción", example = "1")
    Long atraccionId,

    @Schema(description = "Nombre de la atracción", example = "Piscina de Pelotas")
    String atraccionNombre,

    @Schema(description = "ID del menor asignado (si aplica)", example = "5")
    Long menorId,

    @Schema(description = "Nombre del menor (si aplica)", example = "Juanito Gómez")
    String menorNombre,

    @Schema(description = "Tipo de tarifa aplicada", example = "TIEMPO")
    TipoTarifa tipoTarifa,

    @Schema(description = "ID de tarifa por tiempo (si aplica)", example = "10")
    Long tarifaTiempoId,

    @Schema(description = "ID de tarifa por evento (si aplica)", example = "20")
    Long tarifaEventoId,

    @Schema(description = "Cantidad adquirida", example = "1")
    Integer cantidad,

    @Schema(description = "Precio unitario cobrado", example = "15000.00")
    BigDecimal precioUnitario,

    @Schema(description = "Subtotal de la línea", example = "15000.00")
    BigDecimal subtotal
) {}
