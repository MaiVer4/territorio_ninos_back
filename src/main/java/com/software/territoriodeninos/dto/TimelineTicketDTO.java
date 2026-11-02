package com.software.territoriodeninos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record TimelineTicketDTO(
        @Schema(description = "Identificador único UUID de la boleta")
        UUID ticketUuid,

        @Schema(description = "Nombre de la atracción asignada")
        String atraccionNombre,

        @Schema(description = "Nombre del acudiente/comprador")
        String clienteNombre,

        @Schema(description = "Documento del acudiente")
        String clienteDocumento,

        @Schema(description = "Nombre del menor beneficiario")
        String menorNombre,

        @Schema(description = "Estado actual del ticket")
        String estadoActual,

        @Schema(description = "Fecha de emisión original")
        LocalDateTime fechaEmision,

        @Schema(description = "Fecha de consumo efectivo")
        LocalDateTime fechaConsumo,

        @Schema(description = "Línea temporal cronológica de estados y eventos")
        List<EventoTimelineTicketDTO> eventos
) {

    @Builder
    public record EventoTimelineTicketDTO(
            Long id,
            String estadoOrigen,
            String estadoDestino,
            String motivo,
            LocalDateTime fechaEvento,
            String fechaLegible,
            Long usuarioId,
            String usuarioNombre,
            String dispositivoInfo
    ) {}
}
