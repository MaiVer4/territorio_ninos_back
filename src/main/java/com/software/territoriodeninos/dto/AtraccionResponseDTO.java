package com.software.territoriodeninos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AtraccionResponseDTO(
    Long id,
    String nombre,
    String descripcion,
    BigDecimal tarifaMinuto,
    String estado,
    LocalDateTime fechaCreacion,
    String tipo,
    Integer capacidadMaxima,
    Integer duracionMinutosBase,
    Integer toleranciaMinutos,
    Integer caidasLimite,
    Integer edadMinima,
    Integer edadMaxima
) {
    public AtraccionResponseDTO(Long id, String nombre, String descripcion, BigDecimal tarifaMinuto, String estado, LocalDateTime fechaCreacion) {
        this(id, nombre, descripcion, tarifaMinuto, estado, fechaCreacion, "TEMPORIZADA", 1, 15, 0, 3, 0, 12);
    }

    public AtraccionResponseDTO(Long id, String nombre, String descripcion, BigDecimal tarifaMinuto, String estado, LocalDateTime fechaCreacion, String tipo, Integer capacidadMaxima, Integer duracionMinutosBase, Integer toleranciaMinutos, Integer caidasLimite) {
        this(id, nombre, descripcion, tarifaMinuto, estado, fechaCreacion, tipo, capacidadMaxima, duracionMinutosBase, toleranciaMinutos, caidasLimite, 0, 12);
    }
}
