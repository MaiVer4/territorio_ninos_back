package com.software.territoriodeninos.dto;

import com.software.territoriodeninos.util.EstadoReporteAtraccion;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Solicitud para actualizar el estado de un reporte de atracción")
public record ReporteAtraccionEstadoDTO(
    @NotNull(message = "El estado es obligatorio")
    @Schema(description = "Nuevo estado del reporte (ABIERTO, EN_PROCESO, RESUELTO)", example = "RESUELTO")
    EstadoReporteAtraccion estado,

    @Size(max = 1000, message = "Las observaciones no pueden superar los 1000 caracteres")
    @Schema(description = "Observaciones de atención o resolución", example = "Resorte reemplazado y probado")
    String observacionesResolucion,

    @Schema(description = "Si es true y el estado es RESUELTO, reactiva la atracción a ACTIVO", example = "true")
    Boolean reactivarAtraccion
) {}
