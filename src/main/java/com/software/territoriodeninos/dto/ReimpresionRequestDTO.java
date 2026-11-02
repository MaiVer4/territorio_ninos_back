package com.software.territoriodeninos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Solicitud para reimprimir un ticket con justificación obligatoria de auditoría")
public record ReimpresionRequestDTO(
    @NotBlank(message = "El motivo de reimpresión es obligatorio")
    @Size(max = 500, message = "El motivo de reimpresión no puede exceder 500 caracteres")
    @Schema(description = "Motivo o justificación de la reimpresión", example = "Atasco de papel en la impresora térmica")
    String motivo
) {}
