package com.software.territoriodeninos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO para la configuración general del parque")
public class ConfiguracionParqueDTO {

    private Long id;

    @NotBlank(message = "El nombre del parque es obligatorio")
    private String nombreParque;

    @NotBlank(message = "El NIT es obligatorio")
    private String nit;

    private String direccion;

    private String telefono;

    private String pieTicketLegal;

    @NotNull(message = "El tiempo de gracia en segundos es obligatorio")
    private Integer tiempoGraciaGeneralSegundos;
}
