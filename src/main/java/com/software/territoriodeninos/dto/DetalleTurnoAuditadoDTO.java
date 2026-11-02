package com.software.territoriodeninos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record DetalleTurnoAuditadoDTO(
        @Schema(description = "Resumen contable y de conciliación del turno auditado")
        ReporteTurnoDTO turno,

        @Schema(description = "Detalle discriminado de ventas registradas en el turno")
        List<VentaAuditadaDTO> ventas
) {

    @Builder
    public record VentaAuditadaDTO(
            Long ventaId,
            LocalDateTime fechaVenta,
            String clienteNombre,
            String clienteDocumento,
            String metodoPago,
            String estadoVenta,
            BigDecimal montoTotal,
            List<DetalleLineaAuditadaDTO> lineas
    ) {}

    @Builder
    public record DetalleLineaAuditadaDTO(
            Long lineaId,
            Long atraccionId,
            String atraccionNombre,
            String menorNombre,
            String tipoTarifa,
            Integer cantidad,
            BigDecimal precioUnitario,
            BigDecimal subtotal,
            List<TicketAuditadoDTO> boletas
    ) {}

    @Builder
    public record TicketAuditadoDTO(
            UUID uuidCodigo,
            String estadoTicket,
            LocalDateTime fechaEmision,
            LocalDateTime fechaConsumo
    ) {}
}
