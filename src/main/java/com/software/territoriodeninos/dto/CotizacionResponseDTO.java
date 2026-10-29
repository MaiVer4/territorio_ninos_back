package com.software.territoriodeninos.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "DTO de respuesta con el desglose financiero de la cotización")
public record CotizacionResponseDTO(
    @Schema(description = "Lista de subtotales redondeados por cada línea")
    List<BigDecimal> subtotales,

    @Schema(description = "Detalle de ítems cotizados con su subtotal individual")
    List<ItemCotizacionDTO> items,

    @Schema(description = "Suma total de los subtotales antes de descuentos e impuestos", example = "30000.00")
    BigDecimal subtotal,

    @Schema(description = "Descuento aplicado", example = "0.00")
    BigDecimal descuento,

    @Schema(description = "Base gravable sobre la cual se calculan los impuestos", example = "30000.00")
    BigDecimal baseGravable,

    @Schema(description = "Porcentaje de impuesto institucional aplicado", example = "19.00")
    BigDecimal porcentajeImpuesto,

    @Schema(description = "Monto total de impuestos calculados", example = "5700.00")
    BigDecimal impuestos,

    @Schema(description = "Total neto a pagar (baseGravable + impuestos)", example = "35700.00")
    BigDecimal totalNeto
) {
    @JsonProperty("total")
    public BigDecimal total() {
        return totalNeto;
    }
}
