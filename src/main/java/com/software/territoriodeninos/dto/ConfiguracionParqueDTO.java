package com.software.territoriodeninos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO para la configuración general del parque")
public class ConfiguracionParqueDTO {

    private Long id;

    @NotBlank(message = "El nombre del parque es obligatorio")
    @Size(max = 255, message = "El nombre del parque no puede superar 255 caracteres")
    private String nombreParque;

    @Size(max = 255, message = "El nombre comercial no puede superar 255 caracteres")
    private String nombreComercial;

    @Size(max = 255, message = "La razón social no puede superar 255 caracteres")
    private String razonSocial;

    @NotBlank(message = "El NIT es obligatorio")
    @Size(max = 255, message = "El NIT no puede superar 255 caracteres")
    private String nit;

    @Size(max = 255, message = "La dirección no puede superar 255 caracteres")
    private String direccion;

    @Size(max = 255, message = "El teléfono no puede superar 255 caracteres")
    private String telefono;

    @Size(max = 255, message = "La ciudad no puede superar 255 caracteres")
    private String ciudad;

    @Size(max = 2000, message = "Las políticas de seguridad no pueden superar 2000 caracteres")
    private String politicasSeguridad;

    @Min(value = 58, message = "El ancho de papel mínimo es 58 mm")
    @Max(value = 80, message = "El ancho de papel máximo es 80 mm")
    private Integer anchoPapelMm;

    @Size(max = 255, message = "El mensaje de pie no puede superar 255 caracteres")
    private String mensajePie;

    @Size(max = 2000, message = "El pie legal del ticket no puede superar 2000 caracteres")
    private String pieTicketLegal;

    @NotNull(message = "El tiempo de gracia en segundos es obligatorio")
    @Min(value = 0, message = "El tiempo de gracia debe ser mayor o igual a 0")
    private Integer tiempoGraciaGeneralSegundos;

    @Schema(description = "Indica si se aplican impuestos en las ventas del parque", example = "true")
    private Boolean aplicaImpuesto;

    @DecimalMin(value = "0.00", message = "El porcentaje de impuesto no puede ser negativo")
    @DecimalMax(value = "100.00", message = "El porcentaje de impuesto no puede superar 100%")
    @Schema(description = "Porcentaje de impuesto a aplicar (ej. 19.00 para IVA)", example = "19.00")
    private BigDecimal porcentajeImpuesto;
}
