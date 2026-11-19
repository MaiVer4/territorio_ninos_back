package com.software.territoriodeninos.dto;

import com.software.territoriodeninos.util.SeveridadReporte;
import com.software.territoriodeninos.util.TipoReporteAtraccion;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Solicitud para reportar un daño o falta de suplementos en una atracción")
public record ReporteAtraccionRequestDTO(
    @NotNull(message = "El ID de la atracción es obligatorio")
    @Schema(description = "ID de la atracción afectada", example = "1")
    Long atraccionId,

    @NotNull(message = "El tipo de reporte es obligatorio")
    @Schema(description = "Tipo de novedad (DANO, FALTA_SUPLEMENTOS, MANTENIMIENTO_PREVENTIVO, OTRO)", example = "DANO")
    TipoReporteAtraccion tipoReporte,

    @Schema(description = "Severidad del reporte (BAJA, MEDIA, ALTA, CRITICA)", example = "ALTA")
    SeveridadReporte severidad,

    @NotBlank(message = "La descripción del reporte es obligatoria")
    @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
    @Schema(description = "Detalle del daño o suplemento faltante", example = "Se rompió un resorte de la cama elástica #3")
    String descripcion,

    @Schema(description = "Si es true, marca la atracción como FUERA_DE_SERVICIO automáticamente", example = "true")
    Boolean desactivarAtraccion
) {}
