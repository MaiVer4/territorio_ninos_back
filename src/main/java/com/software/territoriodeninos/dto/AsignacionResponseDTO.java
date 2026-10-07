package com.software.territoriodeninos.dto;

import com.software.territoriodeninos.util.EstadoAsignacion;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "DTO de respuesta para asignación de operario")
public record AsignacionResponseDTO(
    Long id,
    Long usuarioId,
    String nombreOperario,
    String correoOperario,
    Long atraccionId,
    String nombreAtraccion,
    LocalDate fechaAsignacion,
    LocalDateTime horaInicio,
    LocalDateTime horaFin,
    EstadoAsignacion estado,
    String observaciones
) {}
