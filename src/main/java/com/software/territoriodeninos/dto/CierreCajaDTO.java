package com.software.territoriodeninos.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "DTO para registrar el cierre de turno de caja")
public record CierreCajaDTO(
    @JsonAlias({"montoFinal", "totalEfectivoReal", "monto_efectivo_reportado", "montoEfectivoReportado"})
    @NotNull(message = "El monto en efectivo reportado es obligatorio")
    @DecimalMin(value = "0.0", inclusive = true, message = "El monto en efectivo no puede ser negativo")
    @Schema(description = "Monto en efectivo real contado en el cajón", example = "350000.00")
    BigDecimal montoEfectivoReportado,

    @Size(max = 1000, message = "Las observaciones no pueden exceder 1000 caracteres")
    @Schema(description = "Observaciones de cierre y justificación en caso de diferencia", example = "Faltante por billete deteriorado")
    String observaciones
) {
    public CierreCajaDTO(BigDecimal montoEfectivoReportado) {
        this(montoEfectivoReportado, null);
    }
}
