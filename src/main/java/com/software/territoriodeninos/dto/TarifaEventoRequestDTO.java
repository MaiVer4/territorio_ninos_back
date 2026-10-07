package com.software.territoriodeninos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "DTO para crear o actualizar una tarifa por evento")
public record TarifaEventoRequestDTO(
    @NotNull(message = "El ID de la atracción es obligatorio")
    @Schema(description = "ID de la atracción asociada", example = "2")
    Long atraccionId,

    @NotNull(message = "El número de intentos es obligatorio")
    @Min(value = 1, message = "Debe tener al menos 1 intento")
    @Schema(description = "Número de intentos (ej: 1, 3)", example = "1")
    Integer intentos,

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
    @Schema(description = "Precio para los intentos", example = "3000.00")
    BigDecimal precio,

    @Schema(description = "Estado de la tarifa (activo/inactivo)", example = "true")
    Boolean activo
) {
    public TarifaEventoRequestDTO(Long atraccionId, Integer intentos, BigDecimal precio) {
        this(atraccionId, intentos, precio, true);
    }
}
