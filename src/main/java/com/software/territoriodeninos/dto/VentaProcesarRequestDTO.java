package com.software.territoriodeninos.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.software.territoriodeninos.util.MetodoPago;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "DTO de solicitud para procesar y registrar una venta en taquilla")
public record VentaProcesarRequestDTO(
    @NotNull(message = "El ID del cliente es obligatorio")
    @JsonAlias({"idCliente", "cliente_id"})
    @Schema(description = "ID del cliente (acudiente) que realiza la compra", example = "1")
    Long clienteId,

    @JsonAlias({"idTurnoCaja", "turno_caja_id"})
    @Schema(description = "ID del turno de caja (opcional si se resuelve del turno activo del cajero en sesión)", example = "3")
    Long turnoCajaId,

    @NotNull(message = "El método de pago es obligatorio")
    @JsonAlias({"metodo_pago", "formaPago", "medioPago"})
    @Schema(description = "Método de pago utilizado (EFECTIVO, TARJETA, TRANSFERENCIA, DATAFONO, NEQUI, DAVIPLATA)", example = "EFECTIVO")
    MetodoPago metodoPago,

    @NotNull(message = "El monto recibido es obligatorio")
    @DecimalMin(value = "0.00", inclusive = true, message = "El monto recibido no puede ser negativo")
    @JsonAlias({"monto_recibido", "efectivoRecibido", "valorRecibido"})
    @Schema(description = "Monto entregado por el cliente para el pago", example = "50000.00")
    BigDecimal montoRecibido,

    @DecimalMin(value = "0.00", inclusive = true, message = "El descuento no puede ser negativo")
    @Schema(description = "Descuento global aplicado a la venta (opcional)", example = "0.00")
    BigDecimal descuento,

    @Size(max = 500, message = "Las observaciones no pueden exceder 500 caracteres")
    @Schema(description = "Observaciones opcionales de la venta", example = "Pago en efectivo en taquilla principal")
    String observaciones,

    @NotEmpty(message = "La venta debe incluir al menos un ítem detallado")
    @Valid
    @JsonAlias({"detalles", "lineas"})
    @Schema(description = "Lista de ítems detallados de la venta")
    List<@Valid DetalleVentaRequestDTO> items
) {
    public VentaProcesarRequestDTO(
            Long clienteId,
            MetodoPago metodoPago,
            BigDecimal montoRecibido,
            List<DetalleVentaRequestDTO> items
    ) {
        this(clienteId, null, metodoPago, montoRecibido, BigDecimal.ZERO, null, items);
    }
}
