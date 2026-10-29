package com.software.territoriodeninos.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "DTO de solicitud para cotizar y validar totales de una venta")
public record CotizacionRequestDTO(
    @Valid
    @JsonAlias({"lineas", "detalles"})
    @Schema(description = "Lista de ítems detallados a cotizar")
    List<ItemCotizacionDTO> items,

    @Schema(description = "Lista alternativa de subtotales por línea para cálculo directo")
    List<BigDecimal> subtotales,

    @DecimalMin(value = "0.00", inclusive = true, message = "El descuento no puede ser negativo")
    @Schema(description = "Descuento global aplicable sobre el subtotal", example = "0.00")
    BigDecimal descuento
) {
    public CotizacionRequestDTO(List<ItemCotizacionDTO> items) {
        this(items, null, BigDecimal.ZERO);
    }

    public CotizacionRequestDTO(List<ItemCotizacionDTO> items, BigDecimal descuento) {
        this(items, null, descuento);
    }
}
