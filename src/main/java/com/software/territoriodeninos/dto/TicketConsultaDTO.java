package com.software.territoriodeninos.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.software.territoriodeninos.util.EstadoTicket;
import com.software.territoriodeninos.util.TipoTarifa;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Información detallada de consulta de un ticket para verificación operativa y reimpresión en taquilla")
public record TicketConsultaDTO(
    @Schema(description = "Código UUID único del ticket", example = "a5e8f496-cbb4-4cf1-8314-87d2efea3559")
    UUID uuidCodigo,

    @Schema(description = "Estado actual del ticket (PAGADO, EN_USO, CONSUMIDO, ANULADO)", example = "PAGADO")
    EstadoTicket estadoTicket,

    @Schema(description = "Minutos contratados según la tarifa de tiempo asociada (null si es por evento)", example = "30")
    Integer minutosContratados,

    @Schema(description = "Fecha y hora exacta de emisión del ticket")
    LocalDateTime fechaEmision,

    @Schema(description = "ID del menor asignado al ticket", example = "10")
    Long menorId,

    @Schema(description = "Nombre completo del menor asignado", example = "Mateo Gómez")
    String menorNombre,

    @Schema(description = "Número de documento del menor asignado", example = "1099887766")
    String menorDocumento,

    @Schema(description = "ID de la atracción contratada", example = "1")
    Long atraccionId,

    @Schema(description = "Nombre de la atracción contratada", example = "Laberinto Mágico")
    String atraccionNombre,

    @Schema(description = "Tipo de tarifa contratada", example = "TIEMPO")
    TipoTarifa tipoTarifa,

    @Schema(description = "ID de la venta asociada", example = "100")
    Long ventaId,

    @Schema(description = "ID del detalle de venta asociado", example = "205")
    Long detalleVentaId,

    @Schema(description = "ID del cliente acudiente titular de la compra", example = "5")
    Long clienteId,

    @Schema(description = "Número de documento (cédula) del cliente acudiente", example = "1098765432")
    String clienteDocumento,

    @Schema(description = "Nombre completo del cliente acudiente", example = "Laura Castro")
    String clienteNombre
) {
    @JsonProperty("id")
    public UUID id() {
        return uuidCodigo;
    }

    @JsonProperty("estadoActual")
    public EstadoTicket estadoActual() {
        return estadoTicket;
    }

    @JsonProperty("horaEmision")
    public LocalDateTime horaEmision() {
        return fechaEmision;
    }
}
