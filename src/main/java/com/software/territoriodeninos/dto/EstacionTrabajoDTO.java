package com.software.territoriodeninos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "DTO con detalles de la estación de trabajo y atracción física asignada al operario (HU-24.02)")
public record EstacionTrabajoDTO(
    @Schema(description = "ID de la asignación operativa", example = "10")
    Long asignacionId,

    @Schema(description = "ID del usuario operario", example = "5")
    Long operarioId,

    @Schema(description = "Nombre completo del operario", example = "Carlos Operario")
    String operarioNombre,

    @Schema(description = "Correo electrónico del operario", example = "operario@territoriodeninos.com")
    String operarioCorreo,

    @Schema(description = "ID de la atracción asignada", example = "1")
    Long atraccionId,

    @Schema(description = "Nombre de la atracción asignada", example = "Piscina de Pelotas")
    String atraccionNombre,

    @Schema(description = "Descripción o reglas de la atracción", example = "Atracción con aforo de 20 niños")
    String atraccionDescripcion,

    @Schema(description = "Tipo de atracción: TEMPORIZADA o EVENTO", example = "TEMPORIZADA")
    String tipoAtraccion,

    @Schema(description = "Capacidad máxima simultánea de niños", example = "20")
    Integer capacidadMaxima,

    @Schema(description = "Aforo u ocupación activa actual de la atracción", example = "5")
    Integer aforoActual,

    @Schema(description = "Cupos disponibles en tiempo real", example = "15")
    Integer cuposDisponibles,

    @Schema(description = "Duración base en minutos", example = "15")
    Integer duracionMinutosBase,

    @Schema(description = "Tolerancia o tiempo de gracia en minutos", example = "5")
    Integer toleranciaMinutos,

    @Schema(description = "Límite de caídas para eventos", example = "3")
    Integer caidasLimite,

    @Schema(description = "Edad mínima permitida en años", example = "3")
    Integer edadMinima,

    @Schema(description = "Edad máxima permitida en años", example = "12")
    Integer edadMaxima,

    @Schema(description = "Rango de edad formateado", example = "3 a 12 años")
    String rangoEdad,

    @Schema(description = "Estado operativo de la atracción", example = "ACTIVO")
    String estadoAtraccion,

    @Schema(description = "Fecha de asignación del turno", example = "2026-11-02")
    LocalDate fechaAsignacion,

    @Schema(description = "Hora de inicio del turno operativo", example = "2026-11-02T08:00:00")
    LocalDateTime horaInicioTurno
) {
    public static String formatearRangoEdad(Integer min, Integer max) {
        if (min != null && max != null) {
            return min + " a " + max + " años";
        } else if (min != null) {
            return "Desde " + min + " años";
        } else if (max != null) {
            return "Hasta " + max + " años";
        }
        return "Todas las edades";
    }
}
