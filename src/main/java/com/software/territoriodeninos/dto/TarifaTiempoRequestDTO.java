package com.software.territoriodeninos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "DTO para crear o actualizar una tarifa por tiempo")
public record TarifaTiempoRequestDTO(
    @NotNull(message = "El ID de la atracción es obligatorio")
    @Schema(description = "ID de la atracción asociada", example = "1")
    Long atraccionId,

    @NotNull(message = "La duración en minutos es obligatoria")
    @Min(value = 1, message = "La duración debe ser mayor a 0 minutos")
    @Schema(description = "Duración en minutos (ej: 15, 30, 60)", example = "15")
    Integer duracionMinutos,

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
    @Schema(description = "Precio para el bloque de tiempo", example = "5000.00")
    BigDecimal precio,

    @Schema(description = "Estado de la tarifa (activo/inactivo)", example = "true")
    Boolean activo
) {
    public TarifaTiempoRequestDTO(Long atraccionId, Integer duracionMinutos, BigDecimal precio) {
        this(atraccionId, duracionMinutos, precio, true);
    }
}
