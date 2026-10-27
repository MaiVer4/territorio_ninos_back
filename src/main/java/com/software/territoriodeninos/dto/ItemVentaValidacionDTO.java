package com.software.territoriodeninos.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Ítem de venta para validación de aforo y consistencia de tarifa")
public record ItemVentaValidacionDTO(
    @NotNull(message = "El ID de la atracción es obligatorio")
    @JsonAlias({"idAtraccion", "atraccion_id"})
    @Schema(description = "ID de la atracción", example = "1")
    Long atraccionId,

    @JsonAlias({"idTarifaTiempo", "tarifa_tiempo_id"})
    @Schema(description = "ID de la tarifa por tiempo (si aplica)", example = "10")
    Long tarifaTiempoId,

    @JsonAlias({"idTarifaEvento", "tarifa_evento_id"})
    @Schema(description = "ID de la tarifa por evento (si aplica)", example = "20")
    Long tarifaEventoId,

    @Min(value = 1, message = "La cantidad de tickets debe ser al menos 1")
    @Schema(description = "Cantidad de tickets/cupos solicitados en esta línea", example = "2")
    Integer cantidad,

    @NotNull(message = "El precio unitario es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio unitario debe ser mayor a 0")
    @JsonAlias({"precio", "valorUnitario", "precio_unitario"})
    @Schema(description = "Precio unitario enviado por el cliente para validación", example = "15000.00")
    BigDecimal precioUnitario,

    @JsonAlias({"totalLinea", "valorTotal"})
    @Schema(description = "Subtotal opcional de la línea (precioUnitario * cantidad)", example = "30000.00")
    BigDecimal subtotal,

    @Schema(description = "Duración en minutos esperada (opcional para doble verificación)", example = "15")
    Integer duracionMinutos,

    @Schema(description = "Número de intentos esperado (opcional para doble verificación)", example = "3")
    Integer intentos
) {
    public ItemVentaValidacionDTO {
        if (cantidad == null) {
            cantidad = 1;
        }
    }
}
