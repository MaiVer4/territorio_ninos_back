package com.software.territoriodeninos.dto;

import com.software.territoriodeninos.util.TipoAtraccion;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AtraccionRequestDTO(
    @NotBlank(message = "El nombre es obligatorio")
    String nombre,
    
    String descripcion,
    
    @NotNull(message = "La tarifa por minuto es obligatoria")
    @DecimalMin(value = "0.01", message = "La tarifa debe ser mayor a 0")
    BigDecimal tarifaMinuto,

    TipoAtraccion tipo,
    @Min(value = 1, message = "La capacidad máxima debe ser al menos 1")
    Integer capacidadMaxima,
    @Min(value = 1, message = "La duración base debe ser al menos 1 minuto")
    Integer duracionMinutosBase,
    Integer toleranciaMinutos,
    Integer caidasLimite,
    @Min(value = 0, message = "La edad mínima no puede ser negativa")
    Integer edadMinima,
    @Min(value = 0, message = "La edad máxima no puede ser negativa")
    Integer edadMaxima
) {
    public AtraccionRequestDTO(String nombre, String descripcion, BigDecimal tarifaMinuto) {
        this(nombre, descripcion, tarifaMinuto, TipoAtraccion.TEMPORIZADA, 1, 15, 0, 3, 0, 12);
    }

    public AtraccionRequestDTO(String nombre, String descripcion, BigDecimal tarifaMinuto, TipoAtraccion tipo, Integer capacidadMaxima, Integer duracionMinutosBase, Integer toleranciaMinutos, Integer caidasLimite) {
        this(nombre, descripcion, tarifaMinuto, tipo, capacidadMaxima, duracionMinutosBase, toleranciaMinutos, caidasLimite, 0, 12);
    }
}
