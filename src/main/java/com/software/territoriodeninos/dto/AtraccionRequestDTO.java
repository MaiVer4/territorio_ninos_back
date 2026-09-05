package com.software.territoriodeninos.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AtraccionRequestDTO(
    @NotBlank(message = "El nombre es obligatorio")
    String nombre,
    
    String descripcion,
    
    @NotNull(message = "La tarifa por minuto es obligatoria")
    @DecimalMin(value = "0.01", message = "La tarifa debe ser mayor a 0")
    BigDecimal tarifaMinuto
) {}
