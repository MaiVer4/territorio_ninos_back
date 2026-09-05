package com.software.territoriodeninos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AtraccionResponseDTO(
    Long id,
    String nombre,
    String descripcion,
    BigDecimal tarifaMinuto,
    String estado,
    LocalDateTime fechaCreacion
) {}
