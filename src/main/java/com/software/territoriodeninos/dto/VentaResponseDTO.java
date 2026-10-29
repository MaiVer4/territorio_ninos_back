package com.software.territoriodeninos.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.software.territoriodeninos.entity.Venta;
import com.software.territoriodeninos.util.EstadoVenta;
import com.software.territoriodeninos.util.MetodoPago;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Comprobante y resultado del procesamiento de una venta en taquilla")
public record VentaResponseDTO(
    @Schema(description = "ID único de la venta", example = "501")
    Long id,

    @Schema(description = "ID del cliente titular", example = "1")
    Long clienteId,

    @Schema(description = "Nombre completo del cliente titular", example = "Carlos Gómez")
    String clienteNombre,

    @Schema(description = "Documento del cliente titular", example = "1094987654")
    String clienteDocumento,

    @Schema(description = "ID del turno de caja", example = "3")
    Long turnoCajaId,

    @Schema(description = "Monto total neto de la venta", example = "35700.00")
    BigDecimal montoTotal,

    @Schema(description = "Subtotal bruto de la venta", example = "30000.00")
    BigDecimal subtotal,

    @Schema(description = "Descuento aplicado", example = "0.00")
    BigDecimal descuento,

    @Schema(description = "Impuestos aplicados (IVA)", example = "5700.00")
    BigDecimal impuestos,

    @Schema(description = "Monto entregado por el cliente", example = "50000.00")
    BigDecimal montoRecibido,

    @Schema(description = "Cambio o vuelto entregado", example = "14300.00")
    BigDecimal cambio,

    @Schema(description = "Método de pago utilizado", example = "EFECTIVO")
    MetodoPago metodoPago,

    @Schema(description = "Estado actual de la venta", example = "COMPLETADA")
    EstadoVenta estadoVenta,

    @Schema(description = "Fecha y hora de registro de la venta", example = "2026-10-29T13:00:00")
    LocalDateTime fechaVenta,

    @Schema(description = "Observaciones de la venta", example = "Pago en efectivo en taquilla principal")
    String observaciones,

    @Schema(description = "Detalle de líneas y boletos emitidos")
    List<DetalleVentaResponseDTO> detalles
) {
    public static VentaResponseDTO fromEntity(Venta v) {
        if (v == null) return null;
        Long cId = v.getCliente() != null ? v.getCliente().getId() : null;
        String cNombre = v.getCliente() != null ? v.getCliente().getNombreCompleto() : null;
        String cDoc = v.getCliente() != null ? v.getCliente().getNumeroDocumento() : null;
        Long tId = v.getTurnoCaja() != null ? v.getTurnoCaja().getId() : null;

        List<DetalleVentaResponseDTO> detallesDTO = v.getDetalles() != null
                ? v.getDetalles().stream().map(DetalleVentaResponseDTO::fromEntity).toList()
                : Collections.emptyList();

        return new VentaResponseDTO(
                v.getId(),
                cId,
                cNombre,
                cDoc,
                tId,
                v.getMontoTotal(),
                v.getSubtotal(),
                v.getDescuento(),
                v.getImpuestos(),
                v.getMontoRecibido(),
                v.getCambio(),
                v.getMetodoPago(),
                v.getEstadoVenta(),
                v.getFechaVenta(),
                v.getObservaciones(),
                detallesDTO
        );
    }
}
