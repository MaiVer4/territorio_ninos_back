package com.software.territoriodeninos.dto.tienda;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Ítem de compra online asignado a un menor")
public record ItemCompraOnlineDTO(
    @NotNull(message = "El ID de la atracción es obligatorio")
    Long atraccionId,

    Long tarifaTiempoId,
    Long tarifaEventoId,

    @NotBlank(message = "El nombre del niño es obligatorio")
    @JsonAlias({"nombreMenor", "nombre"})
    String menorNombre,

    @NotNull(message = "La edad del niño es obligatoria")
    @Min(value = 0, message = "La edad no puede ser negativa")
    @JsonAlias({"edadMenor", "edad"})
    Integer menorEdad,

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad mínima es 1")
    Integer cantidad
) {}
