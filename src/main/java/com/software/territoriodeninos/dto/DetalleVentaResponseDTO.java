package com.software.territoriodeninos.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.software.territoriodeninos.entity.DetalleVenta;
import com.software.territoriodeninos.util.TipoTarifa;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Detalle o línea de boleta emitida en una venta")
public record DetalleVentaResponseDTO(
    @Schema(description = "ID del detalle de venta", example = "101")
    Long id,

    @Schema(description = "ID de la atracción", example = "1")
    Long atraccionId,

    @Schema(description = "Nombre de la atracción", example = "Autos Chocones")
    String atraccionNombre,

    @Schema(description = "ID del menor asignado (opcional)", example = "5")
    Long menorId,

    @Schema(description = "Nombre del menor asignado (opcional)", example = "Mateo Gómez")
    String menorNombre,

    @Schema(description = "Tipo de tarifa aplicada", example = "TIEMPO")
    TipoTarifa tipoTarifa,

    @Schema(description = "ID de tarifa por tiempo (si aplica)", example = "10")
    Long tarifaTiempoId,

    @Schema(description = "ID de tarifa por evento (si aplica)", example = "20")
    Long tarifaEventoId,

    @Schema(description = "Cantidad de boletas emitidas", example = "2")
    Integer cantidad,

    @Schema(description = "Precio unitario oficial", example = "15000.00")
    BigDecimal precioUnitario,

    @Schema(description = "Subtotal de la línea", example = "30000.00")
    BigDecimal subtotal
) {
    public static DetalleVentaResponseDTO fromEntity(DetalleVenta d) {
        if (d == null) return null;
        Long atraccionId = d.getAtraccion() != null ? d.getAtraccion().getId() : null;
        String atraccionNombre = d.getAtraccion() != null ? d.getAtraccion().getNombre() : null;
        Long menorId = d.getMenor() != null ? d.getMenor().getId() : null;
        String menorNombre = d.getMenor() != null ? d.getMenor().getNombreCompleto() : null;

        return new DetalleVentaResponseDTO(
                d.getId(),
                atraccionId,
                atraccionNombre,
                menorId,
                menorNombre,
                d.getTipoTarifa(),
                d.getTarifaTiempoId(),
                d.getTarifaEventoId(),
                d.getCantidad(),
                d.getPrecioUnitario(),
                d.getSubtotal()
        );
    }
}
