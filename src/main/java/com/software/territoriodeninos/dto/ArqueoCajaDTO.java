package com.software.territoriodeninos.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "DTO para registro de arqueo parcial o intermedio de caja")
public record ArqueoCajaDTO(
    @DecimalMin(value = "0.0", message = "El monto recontado no puede ser negativo")
    @JsonAlias({"monto", "montoRecontado", "monto_recontado", "montoFisico"})
    BigDecimal montoEfectivoRecontado,

    String observaciones
) {}
