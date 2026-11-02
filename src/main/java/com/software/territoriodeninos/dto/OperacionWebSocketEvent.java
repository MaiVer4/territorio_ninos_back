package com.software.territoriodeninos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Contrato de mensaje transmitido por WebSocket STOMP a terminales operativas y Dashboard (RNF8)")
public record OperacionWebSocketEvent(
    @Schema(description = "Tipo de evento de operación", example = "NUEVO_INGRESO")
    String tipo,

    @Schema(description = "ID de la atracción afectada", example = "1")
    Long atraccionId,

    @Schema(description = "ID de la sesión de juego relacionada", example = "105")
    Long sesionId,

    @Schema(description = "Mensaje o descripción del evento", example = "Ingreso registrado exitosamente")
    String mensaje,

    @Schema(description = "Datos detallados de la sesión de juego")
    SesionJuegoResponseDTO sesion,

    @Schema(description = "Estado consolidado actualizado de la cola de la atracción")
    ColaAtraccionResponseDTO colaActualizada,

    @Schema(description = "Timestamp del servidor", example = "2026-11-02T15:30:00")
    LocalDateTime timestamp
) {
    public static OperacionWebSocketEvent crear(
            String tipo,
            Long atraccionId,
            Long sesionId,
            String mensaje,
            SesionJuegoResponseDTO sesion,
            ColaAtraccionResponseDTO cola
    ) {
        return new OperacionWebSocketEvent(
                tipo,
                atraccionId,
                sesionId,
                mensaje,
                sesion,
                cola,
                LocalDateTime.now()
        );
    }
}
