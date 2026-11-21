package com.software.territoriodeninos.dto.tienda;

import com.software.territoriodeninos.util.MetodoPago;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Respuesta de compra online procesada con código de orden y tickets digitales")
public record CompraOnlineResponseDTO(
    Long ventaId,
    String codigoOrden,
    String clienteDocumento,
    String clienteNombreCompleto,
    String clienteCorreo,
    BigDecimal subtotal,
    BigDecimal impuestos,
    BigDecimal total,
    MetodoPago metodoPago,
    LocalDateTime fechaCompra,
    List<TicketOnlineDTO> tickets
) {}
