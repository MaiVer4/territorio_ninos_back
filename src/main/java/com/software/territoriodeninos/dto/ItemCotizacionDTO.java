package com.software.territoriodeninos.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Ítem individual dentro de una solicitud de cotización")
public record ItemCotizacionDTO(
    @JsonAlias({"idAtraccion", "atraccion_id"})
    @Schema(description = "ID de la atracción (opcional si se cotiza por monto directo)", example = "1")
    Long atraccionId,

    @JsonAlias({"idTarifaTiempo", "tarifa_tiempo_id"})
    @Schema(description = "ID de tarifa por tiempo", example = "10")
    Long tarifaTiempoId,

    @JsonAlias({"idTarifaEvento", "tarifa_evento_id"})
    @Schema(description = "ID de tarifa por evento", example = "20")
    Long tarifaEventoId,

    @Schema(description = "Concepto o descripción del ítem", example = "Piscina de Pelotas - 15 min")
    String descripcion,

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser mayor o igual a 1")
    @Schema(description = "Cantidad de unidades/tickets", example = "2")
    Integer cantidad,

    @NotNull(message = "El precio unitario es obligatorio")
    @DecimalMin(value = "0.00", inclusive = true, message = "El precio unitario no puede ser negativo")
    @JsonAlias({"precio", "valorUnitario", "precio_unitario"})
    @Schema(description = "Precio unitario antes de impuestos", example = "15000.00")
    BigDecimal precioUnitario,

    @Schema(description = "Subtotal calculado de la línea (usado en respuestas)", example = "30000.00")
    BigDecimal subtotal
) {
    public ItemCotizacionDTO(Long atraccionId, Long tarifaTiempoId, Long tarifaEventoId, String descripcion, Integer cantidad, BigDecimal precioUnitario) {
        this(atraccionId, tarifaTiempoId, tarifaEventoId, descripcion, cantidad, precioUnitario, null);
    }
}
