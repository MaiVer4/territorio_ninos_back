package com.software.territoriodeninos.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.software.territoriodeninos.util.TipoTarifa;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Ítem detallado de una venta a procesar")
public record DetalleVentaRequestDTO(
    @NotNull(message = "El ID de la atracción es obligatorio")
    @JsonAlias({"idAtraccion", "atraccion_id"})
    @Schema(description = "ID de la atracción", example = "1")
    Long atraccionId,

    @JsonAlias({"idMenor", "menor_id"})
    @Schema(description = "ID del menor asignado al boleto", example = "5")
    Long menorId,

    @NotNull(message = "El tipo de tarifa es obligatorio")
    @JsonAlias({"tipo_tarifa", "modalidadTarifa"})
    @Schema(description = "Tipo de tarifa aplicada (TIEMPO, EVENTO, MINUTO_BASE)", example = "TIEMPO")
    TipoTarifa tipoTarifa,

    @JsonAlias({"idTarifaTiempo", "tarifa_tiempo_id"})
    @Schema(description = "ID de la tarifa por tiempo seleccionada (si aplica)", example = "10")
    Long tarifaTiempoId,

    @JsonAlias({"idTarifaEvento", "tarifa_evento_id"})
    @Schema(description = "ID de la tarifa por evento seleccionada (si aplica)", example = "20")
    Long tarifaEventoId,

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    @Schema(description = "Cantidad de boletos en esta línea", example = "1")
    Integer cantidad,

    @NotNull(message = "El precio unitario es obligatorio")
    @DecimalMin(value = "0.00", inclusive = true, message = "El precio unitario no puede ser negativo")
    @JsonAlias({"precio", "valorUnitario", "precio_unitario"})
    @Schema(description = "Precio unitario de la tarifa", example = "15000.00")
    BigDecimal precioUnitario,

    @DecimalMin(value = "0.00", inclusive = true, message = "El subtotal no puede ser negativo")
    @Schema(description = "Subtotal de la línea (precioUnitario * cantidad)", example = "15000.00")
    BigDecimal subtotal
) {
    public DetalleVentaRequestDTO {
        if (cantidad == null) {
            cantidad = 1;
        }
    }

    public DetalleVentaRequestDTO(
            Long atraccionId,
            Long menorId,
            TipoTarifa tipoTarifa,
            Long tarifaTiempoId,
            Long tarifaEventoId,
            Integer cantidad,
            BigDecimal precioUnitario
    ) {
        this(atraccionId, menorId, tipoTarifa, tarifaTiempoId, tarifaEventoId, cantidad, precioUnitario,
                precioUnitario != null && cantidad != null ? precioUnitario.multiply(BigDecimal.valueOf(cantidad)) : null);
    }
}
