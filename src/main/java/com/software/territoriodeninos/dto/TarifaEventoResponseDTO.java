package com.software.territoriodeninos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "DTO de respuesta de una tarifa por evento")
public record TarifaEventoResponseDTO(
    Long id,
    Long atraccionId,
    String nombreAtraccion,
    Integer intentos,
    BigDecimal precio,
    Boolean activo,
    LocalDateTime fechaCreacion
) {}
