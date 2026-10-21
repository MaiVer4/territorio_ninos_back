package com.software.territoriodeninos.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "DTO para la apertura de turno de caja")
public record AperturaCajaDTO(
    @JsonAlias({"montoInicial", "monto_base", "montoBase", "base"})
    @NotNull(message = "El monto base es obligatorio")
    @DecimalMin(value = "0.0", inclusive = true, message = "El monto base no puede ser negativo")
    @Schema(description = "Monto en efectivo con el que se abre la caja", example = "50000.00")
    BigDecimal montoBase,

    @Size(max = 500, message = "Las observaciones no pueden exceder 500 caracteres")
    @Schema(description = "Observaciones iniciales de la apertura", example = "Turno mañana")
    String observaciones
) {
    public AperturaCajaDTO(BigDecimal montoBase) {
        this(montoBase, null);
    }
}
