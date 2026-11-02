package com.software.territoriodeninos.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.software.territoriodeninos.util.EstadoVenta;
import com.software.territoriodeninos.util.MetodoPago;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Respuesta de una venta procesada y congelada en taquilla con tickets emitidos")
public record VentaResponseDTO(
    @Schema(description = "ID único de la venta generada", example = "100")
    Long id,

    @Schema(description = "ID del cliente", example = "1")
    Long clienteId,

    @Schema(description = "Número de documento del cliente", example = "1098765432")
    String clienteDocumento,

    @Schema(description = "Nombre completo del cliente", example = "Laura Castro")
    String clienteNombreCompleto,

    @Schema(description = "ID del turno de caja vinculado", example = "3")
    Long turnoCajaId,

    @Schema(description = "Subtotal antes de descuentos e impuestos", example = "30000.00")
    BigDecimal subtotal,

    @Schema(description = "Descuento aplicado", example = "0.00")
    BigDecimal descuento,

    @Schema(description = "Monto de impuestos calculados", example = "5700.00")
    BigDecimal impuestos,

    @Schema(description = "Monto total neto a pagar", example = "35700.00")
    BigDecimal montoTotal,

    @Schema(description = "Monto entregado por el cliente", example = "50000.00")
    BigDecimal montoRecibido,

    @Schema(description = "Cambio o devuelta entregada al cliente", example = "14300.00")
    BigDecimal cambio,

    @Schema(description = "Método de pago utilizado", example = "EFECTIVO")
    MetodoPago metodoPago,

    @Schema(description = "Estado de la transacción", example = "PAGADO")
    EstadoVenta estadoVenta,

    @Schema(description = "Fecha y hora exacta de emisión de la venta")
    LocalDateTime fechaVenta,

    @Schema(description = "Indica si la transacción está congelada e inmutable", example = "true")
    Boolean congelada,

    @Schema(description = "Observaciones registradas en la venta")
    String observaciones,

    @Schema(description = "Detalles de los ítems de venta adquiridos")
    List<DetalleVentaResponseDTO> detalles,

    @Schema(description = "Tickets individuales emitidos con UUID criptoseguro para cada unidad vendida")
    List<TicketResponseDTO> tickets
) {
    public VentaResponseDTO {
        if (detalles == null) {
            detalles = List.of();
        }
        if (tickets == null) {
            tickets = List.of();
        }
    }

    public VentaResponseDTO(
            Long id, Long clienteId, String clienteDocumento, String clienteNombreCompleto, Long turnoCajaId,
            BigDecimal subtotal, BigDecimal descuento, BigDecimal impuestos, BigDecimal montoTotal,
            BigDecimal montoRecibido, BigDecimal cambio,
            MetodoPago metodoPago, EstadoVenta estadoVenta, LocalDateTime fechaVenta,
            Boolean congelada, String observaciones, List<DetalleVentaResponseDTO> detalles
    ) {
        this(id, clienteId, clienteDocumento, clienteNombreCompleto, turnoCajaId,
                subtotal, descuento, impuestos, montoTotal, montoRecibido, cambio,
                metodoPago, estadoVenta, fechaVenta, congelada, observaciones, detalles, List.of());
    }

    public VentaResponseDTO(
            Long id, Long clienteId, String clienteNombreCompleto, String clienteDocumento, Long turnoCajaId,
            BigDecimal subtotal, BigDecimal montoTotal, BigDecimal descuento, BigDecimal impuestos,
            BigDecimal montoRecibido, BigDecimal cambio,
            MetodoPago metodoPago, EstadoVenta estadoVenta, LocalDateTime fechaVenta, String observaciones,
            List<DetalleVentaResponseDTO> detalles
    ) {
        this(id, clienteId, clienteDocumento, clienteNombreCompleto, turnoCajaId,
                subtotal, descuento, impuestos, montoTotal, montoRecibido, cambio,
                metodoPago, estadoVenta, fechaVenta, true, observaciones, detalles, List.of());
    }

    @JsonProperty("ventaId")
    public Long ventaId() {
        return id;
    }

    @JsonProperty("clienteNombre")
    public String clienteNombre() {
        return clienteNombreCompleto;
    }
}
