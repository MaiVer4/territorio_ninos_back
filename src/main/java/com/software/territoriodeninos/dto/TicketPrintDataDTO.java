package com.software.territoriodeninos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder(toBuilder = true)
@Schema(description = "Datos consolidados para el formateo de impresión térmica de un ticket")
public record TicketPrintDataDTO(
    @Schema(description = "UUID único criptoseguro del ticket", example = "a5e8f496-cbb4-4cf1-8314-87d2efea3559")
    UUID uuidTicket,

    @Schema(description = "Nombre de la atracción", example = "Piscina de Pelotas")
    String nombreAtraccion,

    @Schema(description = "Nombre del menor asignado o 'Sin asignar'", example = "Sofia Restrepo")
    String nombreMenor,

    @Schema(description = "Tiempo o modalidad de la atracción", example = "30 min")
    String tiempoOModalidad,

    @Schema(description = "Precio unitario pagado", example = "30000.00")
    BigDecimal precioUnitario,

    @Schema(description = "Fecha y hora de emisión del ticket")
    LocalDateTime fechaEmision,

    @Schema(description = "Indica si la impresión corresponde a una reimpresión auditada", example = "false")
    Boolean esReimpresion
) {
    public TicketPrintDataDTO(
        UUID uuidTicket,
        String nombreAtraccion,
        String nombreMenor,
        String tiempoOModalidad,
        BigDecimal precioUnitario,
        LocalDateTime fechaEmision
    ) {
        this(uuidTicket, nombreAtraccion, nombreMenor, tiempoOModalidad, precioUnitario, fechaEmision, false);
    }

    public boolean esReimpresionSegura() {
        return Boolean.TRUE.equals(this.esReimpresion);
    }
}
