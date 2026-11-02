package com.software.territoriodeninos.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.software.territoriodeninos.util.EstadoTicket;
import com.software.territoriodeninos.util.TipoTarifa;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Respuesta detallada de un ticket emitido con UUID criptoseguro individual")
public record TicketResponseDTO(
    @Schema(description = "Identificador único criptoseguro del ticket (UUID v4)", example = "a5e8f496-cbb4-4cf1-8314-87d2efea3559")
    UUID uuidCodigo,

    @Schema(description = "ID del detalle de venta asociado", example = "10")
    Long detalleVentaId,

    @Schema(description = "ID de la atracción", example = "1")
    Long atraccionId,

    @Schema(description = "Nombre de la atracción", example = "Laberinto Mágico")
    String atraccionNombre,

    @Schema(description = "ID del menor asignado (opcional)", example = "5")
    Long menorId,

    @Schema(description = "Nombre completo del menor asignado (opcional)", example = "Mateo Gómez")
    String menorNombre,

    @Schema(description = "Tipo de tarifa aplicada", example = "TIEMPO")
    TipoTarifa tipoTarifa,

    @Schema(description = "Estado actual del ticket", example = "PAGADO")
    EstadoTicket estadoTicket,

    @Schema(description = "Fecha y hora exacta de emisión del ticket")
    LocalDateTime fechaEmision
) {
    @JsonProperty("id")
    public UUID id() {
        return uuidCodigo;
    }
}
